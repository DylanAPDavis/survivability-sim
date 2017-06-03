package netlab.storage.controller;

import lombok.extern.slf4j.Slf4j;
import netlab.analysis.analyzed.AnalyzedSet;
import netlab.analysis.services.AnalysisService;
import netlab.storage.services.StorageService;
import netlab.submission.request.RequestSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Slf4j
@Controller
public class StorageController {

    @Autowired
    private StorageController(StorageService storageService){
        this.storageService = storageService;
    }

    private StorageService storageService;

    @RequestMapping(method = RequestMethod.GET, value = "/storage/raw/{requestSetId}/{useAwes}")
    public RequestSet getRequestSet(@PathVariable String requestSetId, @PathVariable Boolean useAws){

        return storageService.retrieveRequestSet(requestSetId, useAws);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/storage/analyzed/{requestSetId}/{useAwes}")
    public AnalyzedSet getAnalyzedSet(@PathVariable String requestSetId, @PathVariable Boolean useAws){

        return storageService.retrieveAnalyzedSet(requestSetId, useAws);
    }
}
