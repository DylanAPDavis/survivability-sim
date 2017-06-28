package netlab;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import netlab.storage.controller.StorageController;
import netlab.submission.controller.SubmissionController;
import netlab.submission.request.RequestSet;
import netlab.submission.request.SimulationParameters;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import java.io.IOException;


@SpringBootApplication
@EnableConfigurationProperties
@Slf4j
public class SurvivabilitySimApplication {

	public static void main(String[] args) {
		Boolean webValue = false;
		SimulationParameters simParams = null;
		String usage = "Usage: --server.port=<port num> --web=<true or false> --sim={JSON representation of SimulationParameter Class}";
		String message = "Startup Arguments: ";
		for (String arg : args) {
			message += arg + ", ";
		}
		log.info(usage);
		log.info(message);
		for (String arg : args) {
			String[] splitArg = arg.split("=");
			String option = splitArg[0];
			String value = splitArg[1];
			if (option.contains("--web")) {
				webValue = Boolean.parseBoolean(value);
			}
			if (option.contains("--sim")) {
				ObjectMapper mapper = new ObjectMapper();
				try {
					simParams = mapper.readValue(value, SimulationParameters.class);
					log.info("Params: " + simParams.toString());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		ConfigurableApplicationContext ctx = new SpringApplicationBuilder(SurvivabilitySimApplication.class)
				.web(webValue ? WebApplicationType.SERVLET : WebApplicationType.NONE)
				.run(args);

		// If they provided simulation parameters, just run the simulation, output results, and shutdown
		if(simParams != null){
			SubmissionController subCon = ctx.getBean(SubmissionController.class);
			StorageController storCon = ctx.getBean(StorageController.class);
			String requestId = subCon.submitRequestSet(simParams);
			RequestSet requestSet = storCon.getRequestSet(requestId, simParams.getUseAws());
			log.info("Request Set ID: " + requestId);
			//log.info("Request Set Details: " + requestSet.toString());
			ctx.close();
			System.exit(0);
		}
	}
}