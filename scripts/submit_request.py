import sys
import json
import requests


portNum = "9867"

if len(sys.argv) != 25:
    print(sys.argv)
    message = "Usage: seed topologyId numRequests algorithm problemClass objective numSources numDestinations"
    message += " failureSetSize failureSetSizeRange[min, max] failureClass failureProb failureProbRange[min, max]"
    message += " numConnections minConnectionsRange[min, max] maxConnectionsRange[min, max]"
    message += " numFailsAllowed numFailsAllowedRange[min, max] processingType sdn useAWS percentSrcAlsoDest"
    message += " percentSrcFail percentDstFail"
    print(message)
    sys.exit(-1)

url = 'http://localhost:' + portNum + '/submit_sim'
# head = {'Content-type': 'application/json', 'Accept': 'application/json'}
payload = {
    'seed': sys.argv[0],
    'topologyId': sys.argv[1],
    'numRequests': sys.argv[2],
    'algorithm': sys.argv[3],
    'problemClass': sys.argv[4],
    'objective': sys.argv[5],
    'numSources': sys.argv[6],
    'numDestinations': sys.argv[7],
    'failureSetSize': sys.argv[8],
    'minMaxFailures': sys.argv[9],
    'failureClass': sys.argv[10],
    'failureProb': sys.argv[11],
    'minMaxFailureProb': sys.argv[12],
    'numConnections': sys.argv[13],
    'minConnectionsRange': sys.argv[14],
    'maxConnectionsRange': sys.argv[15],
    'numFailsAllowed': sys.argv[16],
    'minMaxFailsAllowed': sys.argv[17],
    'processingType': sys.argv[18],
    'percentSrcAlsoDest': sys.argv[19],
    'percentSrcFail': sys.argv[20],
    'percentDstFail': sys.argv[21],
    'sdn': sys.argv[22],
    'useAws': sys.argv[23],
}

jsonPayload = json.dumps(payload)
ret = requests.post(url, data=jsonPayload)
print(ret.text)

