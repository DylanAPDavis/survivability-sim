import sys
import json
import requests

default_len = 30
len_with_port = default_len + 1


def submit(args):
    num_args = len(args)
    if num_args != default_len and num_args != len_with_port:
        print(args)
        message = "Usage: seed topologyId numRequests algorithm problemClass objective numSources numDestinations"
        message += " failureSetSize failureSetSizeRange[min, max] failureClass failureProb failureProbRange[min, max]"
        message += " numConnections minConnectionsRange[min, max] maxConnectionsRange[min, max]"
        message += " minSrcConnectionsRange[min, max] maxSrcConnectionsRange[min, max]"
        message += " minDstConnectionsRange[min, max] maxDstConnectionsRange[min, max]"
        message += " numFailsAllowed numFailsAllowedRange[min, max] processingType percentSrcAlsoDest"
        message += " percentSrcFail percentDstFail sdn useAWS ignoreFailures numThreads portNum(optional)"
        print(message)
        sys.exit(-1)

    port_num = args[-1] if num_args == len_with_port else "9867"
    url = 'http://localhost:' + port_num + '/submit_sim'
    payload = {
        'seed': args[0],
        'topologyId': args[1],
        'numRequests': args[2],
        'algorithm': args[3],
        'problemClass': args[4],
        'objective': args[5],
        'numSources': args[6],
        'numDestinations': args[7],
        'failureSetSize': args[8],
        'minMaxFailures': args[9],
        'failureClass': args[10],
        'failureProb': args[11],
        'minMaxFailureProb': args[12],
        'numConnections': args[13],
        'minConnectionsRange': args[14],
        'maxConnectionsRange': args[15],
        'minSrcConnectionsRange': args[16],
        'maxSrcConnectionsRange': args[17],
        'minDstConnectionsRange': args[18],
        'maxDstConnectionsRange': args[19],
        'numFailsAllowed': args[20],
        'minMaxFailsAllowed': args[21],
        'processingType': args[22],
        'percentSrcAlsoDest': args[23],
        'percentSrcFail': args[24],
        'percentDstFail': args[25],
        'sdn': args[26],
        'useAws': args[27],
        'ignoreFailures': args[28],
        'numThreads': args[29]
    }

    json_payload = json.dumps(payload)
    ret = requests.post(url, data=json_payload)
    print(ret.text)

if __name__ == "__main__":
    submit(sys.argv[1:])
