package org.example.bulk;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosDiagnosticsHandler;
import com.azure.cosmos.CosmosDiagnosticsThresholds;
import com.azure.cosmos.implementation.TestConfigurations;
import com.azure.cosmos.models.CosmosBulkExecutionOptions;
import com.azure.cosmos.models.CosmosBulkOperations;
import com.azure.cosmos.models.CosmosClientTelemetryConfig;
import com.azure.cosmos.models.CosmosItemOperation;
import com.azure.cosmos.models.CosmosMetricCategory;
import com.azure.cosmos.models.CosmosMetricTagName;
import com.azure.cosmos.models.CosmosMicrometerMetricsOptions;
import com.azure.cosmos.models.PartitionKey;
import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.MetricRegistry;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.dropwizard.DropwizardConfig;
import io.micrometer.core.instrument.dropwizard.DropwizardMeterRegistry;
import io.micrometer.core.instrument.util.HierarchicalNameMapper;
import io.micrometer.core.lang.Nullable;
import org.example.entity.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class BulkExecutionSample {

    private static final Logger logger = LoggerFactory.getLogger(BulkExecutionSample.class);

    private static final Duration NON_POINT_OP_DIAGNOSTIC_LOGGING_THRESHOLD = Duration.ofSeconds(4);
    private static final int INITIAL_MICRO_BATCH_SIZE = 1;
    private static final int MAX_MICRO_BATCH_SIZE = 100;

    public static void main(String[] args) {
        try (CosmosAsyncClient asyncClient = buildCosmosAsyncClient()) {

            CosmosAsyncContainer asyncContainer = asyncClient.getDatabase("bulkDemoDb").getContainer("bulkDemoContainer");
            CosmosBulkExecutionOptions cosmosBulkExecutionOptions = new CosmosBulkExecutionOptions();

            cosmosBulkExecutionOptions.setInitialMicroBatchSize(INITIAL_MICRO_BATCH_SIZE);
            cosmosBulkExecutionOptions.setMaxMicroBatchSize(MAX_MICRO_BATCH_SIZE);

            Flux<CosmosItemOperation> cosmosItemOperationFlux = Flux.range(1, 5_000)
                    .flatMap(i -> {
                        Item item = new Item();
                        item.setId(i.toString());

                        return Mono.just(CosmosBulkOperations.getUpsertItemOperation(item, new PartitionKey(item.getId())));
                    });

            asyncContainer.executeBulkOperations(cosmosItemOperationFlux, cosmosBulkExecutionOptions).blockLast();

        } finally {
            logger.info("Bulk ingestion completed!");
        }
    }

    private static CosmosAsyncClient buildCosmosAsyncClient() {

        String endpoint = TestConfigurations.HOST;
        String masterKey = TestConfigurations.MASTER_KEY;

        CosmosClientTelemetryConfig telemetryConfig = new CosmosClientTelemetryConfig();
        CosmosMicrometerMetricsOptions metricsOptions = new CosmosMicrometerMetricsOptions();
        metricsOptions.setMetricCategories(CosmosMetricCategory.DEFAULT, CosmosMetricCategory.OPERATION_DETAILS);

        metricsOptions.configureDefaultTagNames(
                CosmosMetricTagName.DEFAULT,
                CosmosMetricTagName.OPERATION_SUB_STATUS_CODE,
                CosmosMetricTagName.PARTITION_KEY_RANGE_ID);

        final MetricRegistry dropwizardRegistry = new MetricRegistry();
        File metricsFile = new File("<directory path to write client metrics to>");
        final CsvReporter csvReporter = CsvReporter.forRegistry(dropwizardRegistry)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .convertRatesTo(TimeUnit.SECONDS)
                .build(metricsFile);
        csvReporter.start(60, TimeUnit.SECONDS);

        DropwizardConfig dropwizardConfig = new DropwizardConfig() {
            @Override
            public String get(@Nullable String key) {
                return null;
            }
            @Override
            public String prefix() {
                return "csv";
            }
        };

        DropwizardMeterRegistry dropwizardMeterRegistry = new DropwizardMeterRegistry(
                dropwizardConfig,
                dropwizardRegistry,
                HierarchicalNameMapper.DEFAULT,
                Clock.SYSTEM) {
            @Override
            protected Double nullGaugeValue() {
                return Double.NaN;
            }

            @Override
            public void close() {
                super.close();
                csvReporter.stop();
                csvReporter.close();
            }
        };

        dropwizardMeterRegistry.config().meterFilter(
                MeterFilter.ignoreTags(CosmosMetricTagName.CONTAINER.toString(),
                        CosmosMetricTagName.CLIENT_CORRELATION_ID.toString(),
                        CosmosMetricTagName.REGION_NAME.toString())
        );

        metricsOptions.meterRegistry(dropwizardMeterRegistry);
        telemetryConfig.metricsOptions(metricsOptions);
        CosmosClientBuilder cosmosClientBuilder = new CosmosClientBuilder();

        cosmosClientBuilder = cosmosClientBuilder
                .endpoint(endpoint)
                .key(masterKey)
                .preferredRegions(Arrays.asList("South Central US", "East US"))
                .consistencyLevel(ConsistencyLevel.SESSION)
                .clientTelemetryConfig(new CosmosClientTelemetryConfig()
                        .diagnosticsHandler(CosmosDiagnosticsHandler.DEFAULT_LOGGING_HANDLER)
                        .diagnosticsThresholds(
                                new CosmosDiagnosticsThresholds()
                                        .setNonPointOperationLatencyThreshold(NON_POINT_OP_DIAGNOSTIC_LOGGING_THRESHOLD))
                        .metricsOptions(metricsOptions)
                )
                .gatewayMode();

        return cosmosClientBuilder.buildAsyncClient();
    }
}
