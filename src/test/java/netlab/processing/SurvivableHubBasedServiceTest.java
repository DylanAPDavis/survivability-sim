package netlab.processing;

import netlab.TestConfiguration;
import netlab.analysis.analyzed.Analysis;
import netlab.analysis.services.AnalysisService;
import netlab.submission.request.Request;
import netlab.submission.request.SimulationParameters;
import netlab.submission.services.GenerationService;
import netlab.visualization.PrintingService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
public class SurvivableHubBasedServiceTest {

    @Autowired
    ProcessingService processingService;

    @Autowired
    PrintingService printingService;

    @Autowired
    GenerationService generationService;

    @Autowired
    AnalysisService analysisService;

    @Test
    public void anycastTestQuake2(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("tw")
                .algorithm("survivablehub")
                .objective("totalcost")
                .routingType("anycast")
                .numSources(1)
                .numDestinations(3)
                .useMinS(1)
                .useMinD(1)
                .failureScenario("quake2")
                .numFailureEvents(2)
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
        Analysis analysis = analysisService.analyzeRequest(request);
        System.out.print(analysis);
    }

    @Test
    public void manyToManyTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("tw")
                .algorithm("survivablehub")
                .objective("totalcost")
                .routingType("manytomany")
                .numSources(10)
                .numDestinations(3)
                .useMinS(10)
                .useMinD(1)
                .failureScenario("alllinks")
                .numFailureEvents(2)
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
        Analysis analysis = analysisService.analyzeRequest(request);
        System.out.print(analysis);
    }

    @Test
    public void manyToManyTestQuake2(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("tw")
                .algorithm("survivablehub")
                .objective("totalcost")
                .routingType("manytomany")
                .numSources(10)
                .numDestinations(3)
                .useMinS(10)
                .useMinD(1)
                .failureScenario("quake2")
                .numFailureEvents(2)
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
        Analysis analysis = analysisService.analyzeRequest(request);
        System.out.print(analysis);
    }

    @Test
    public void manyToManyTestQuake123(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("tw")
                .algorithm("survivablehub")
                .objective("totalcost")
                .routingType("manytomany")
                .numSources(10)
                .numDestinations(3)
                .useMinS(10)
                .useMinD(1)
                .failureScenario("quake123")
                .numFailureEvents(2)
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
        Analysis analysis = analysisService.analyzeRequest(request);
        System.out.print(analysis);
    }

    @Test
    public void manyToManyTestAllNodes1(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("tw")
                .algorithm("survivablehub")
                .objective("totalcost")
                .routingType("manytomany")
                .numSources(10)
                .numDestinations(3)
                .useMinS(10)
                .useMinD(1)
                .failureScenario("allnodes")
                .numFailureEvents(1)
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
        Analysis analysis = analysisService.analyzeRequest(request);
        System.out.print(analysis);
    }

}
