package netlab.submission;

import netlab.submission.request.SimulationParameters;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class SubmissionRunner {

    private static String portNum = "9867";

    public static void main(String[] args){
        if(args.length != 24){
            String message = "Usage: seed topologyId numRequests algorithm problemClass objective numSources numDestinations";
            message += " failureSetSize failureSetSizeRange[min, max] failureClass failureProb failureProbRange[min, max]";
            message += " numConnections minConnectionsRange[min, max] maxConnectionsRange[min, max]";
            message += " numFailsAllowed numFailsAllowedRange[min, max] processingType sdn useAWS percentSrcAlsoDest";
            message += " percentSrcFail percentDstFail";
            System.out.println(message);
            System.exit(-1);
        }
        SimulationParameters params = populateParams(args);
        if(params == null){
            System.exit(-1);
        }

        // Now submit these parameters to the SubmissionController
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<SimulationParameters> request = new HttpEntity<>(params);
        ResponseEntity<String> response = restTemplate
                .exchange("localhost:" + portNum + "/submit_sim", HttpMethod.POST, request, String.class);
        System.out.println(response.getHeaders());
        System.out.println(response.getStatusCode() + " --" + response.getStatusCodeValue());
        System.out.println(response.getBody());
    }

    private static SimulationParameters populateParams(String[] args) {
        try{
            Long seed = Long.parseLong(args[0]);
            String topologyId = args[1];
            Integer numRequests = Integer.parseInt(args[2]);
            String algorithm = args[3];
            String problemClass = args[4];
            String objective = args[5];
            Integer numSources = Integer.parseInt(args[6]);
            Integer numDestinations = Integer.parseInt(args[7]);
            Integer failureSetSize = Integer.parseInt(args[8]);
            List<Integer> minMaxFailureSetSize = parseListInt(args[9]);
            String failureClass = args[10];
            Double failureProb = Double.parseDouble(args[11]);
            List<Double> minMaxFailureProb = parseListDouble(args[12]);
            Integer numConnections = Integer.parseInt(args[13]);
            List<Integer> minConnectionsRange = parseListInt(args[14]);
            List<Integer> maxConnectionsRange = parseListInt(args[15]);
            Integer numFailsAllowed = Integer.parseInt(args[16]);
            List<Integer> minMaxFailsAllowed = parseListInt(args[17]);
            String processingType = args[18];
            Boolean sdn = Boolean.parseBoolean(args[19]);
            Boolean useAws = Boolean.parseBoolean(args[20]);
            Double percentSrcAlsoDest = Double.parseDouble(args[21]);
            Double percentSrcFail = Double.parseDouble(args[22]);
            Double percentDstFail = Double.parseDouble(args[23]);

            return makeParameters(seed, topologyId, numRequests, algorithm, problemClass, objective, numSources, numDestinations,
                    failureSetSize, minMaxFailureSetSize, failureClass, failureProb, minMaxFailureProb, numConnections, minConnectionsRange,
                    maxConnectionsRange, numFailsAllowed, minMaxFailsAllowed, processingType, sdn, useAws, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        } catch (Exception e){
            System.out.println("Parsing exception, incorrect format for one or more arguments");
            return null;
        }
    }

    private static List<Integer> parseListInt(String arg) {
        // String format: [min, max]
        arg = arg.replace("[]", "").replace(" ", "");
        String[] args = arg.split(",");
        if(args.length == 2){
            try{
                Integer min = Integer.parseInt(args[0]);
                Integer max = Integer.parseInt(args[1]);
                return Arrays.asList(min, max);
            } catch(Exception e){
                System.out.println("Error parsing a range field");
            }
        }
        return new ArrayList<>();
    }

    private static List<Double> parseListDouble(String arg) {
        // String format: [min, max]
        arg = arg.replace("[]", "").replace(" ", "");
        String[] args = arg.split(",");
        if(args.length == 2){
            try{
                Double min = Double.parseDouble(args[0]);
                Double max = Double.parseDouble(args[1]);
                return Arrays.asList(min, max);
            } catch(Exception e){
                System.out.println("Error parsing a range field");
            }
        }
        return new ArrayList<>();
    }

    private static SimulationParameters makeParameters(Long seed, String topologyId, Integer numRequests, String alg, String problemClass,
                                                       String objective, Integer numSources, Integer numDestinations, Integer fSetSize,
                                                       List<Integer> minMaxFailures, String failureClass, Double failureProb,
                                                       List<Double> minMaxFailureProb, Integer numConnections,
                                                       List<Integer> minConnectionsRange, List<Integer> maxConnectionsRange,
                                                       Integer numFails, List<Integer> minMaxFailsAllowed, String processingType, Boolean sdn,
                                                       Boolean useAws, double percentSrcAlsoDest, double percentSrcFail,
                                                       double percentDstFail){
        return SimulationParameters.builder()
                .seed(seed)
                .topologyId(topologyId)
                .numRequests(numRequests)
                .algorithm(alg)
                .problemClass(problemClass)
                .objective(objective)
                .numSources(numSources)
                .numDestinations(numDestinations)
                .failureSetSize(fSetSize)
                .minMaxFailures(minMaxFailures)
                .failureClass(failureClass)
                .failureProb(failureProb)
                .minMaxFailureProb(minMaxFailureProb)
                .numConnections(numConnections)
                .minConnectionsRange(minConnectionsRange)
                .maxConnectionsRange(maxConnectionsRange)
                .numFailsAllowed(numFails)
                .minMaxFailsAllowed(minMaxFailsAllowed)
                .processingType(processingType)
                .sdn(sdn)
                .useAws(useAws)
                .percentSrcAlsoDest(percentSrcAlsoDest)
                .percentSrcFail(percentSrcFail)
                .percentDestFail(percentDstFail)
                .build();
    }
}
