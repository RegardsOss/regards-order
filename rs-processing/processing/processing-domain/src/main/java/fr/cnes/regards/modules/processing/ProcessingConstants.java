package fr.cnes.regards.modules.processing;

public interface ProcessingConstants {

    interface Path {
        interface Param {
            String PROCESS_BUSINESS_ID_PARAM = "processBusinessId";
        }

        String APIV1 = "/api/v1";

        String PROCESS_PATH = APIV1 + "/process";
        String BATCH_PATH = APIV1 + "/batch";

        String PROCESS_CONFIG_PATH = PROCESS_PATH + "/config";
        String PROCESS_CONFIG_BID_PATH = PROCESS_CONFIG_PATH + "/{" + Param.PROCESS_BUSINESS_ID_PARAM + "}" ;

        String PROCESS_METADATA_PATH = PROCESS_PATH + "/metadata";
        String PROCESS_CONFIG_INSTANCES_PATH = PROCESS_CONFIG_PATH + "/instances";

        String MONITORING_EXECUTIONS_PATH = APIV1 + "/monitoring/executions";

        static String param(String name) { return "{" + name + "}"; }
    }

    interface ContentType {
        String APPLICATION_JSON = "application/json";
    }
}