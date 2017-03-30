import requests
import json
import sys

ip = "localhost"
port = "8080"
#serviceName = "trunk_war_exploded"
serviceName = "pmes"
serviceUrl = "http://" + ip + ":" + port + "/" + serviceName + "/"


def get(url, headers):
    r = requests.get(url, headers=headers)
    return r.json()


def post(url, headers, data=None):
    r = requests.post(url, headers=headers, json=data)
    return r.json()


def getSystemStatus():
    # curl -H 'Accept: application/json' http://localhost:8080/trunk_war_exploded/pmes/getSystemStatus -v
    url = serviceUrl + "pmes/getSystemStatus"
    headers = {'Accept': 'application/json'}
    result = get(url, headers)
    return result


def terminateActivity(listOfJobsId):
    url = serviceUrl + "pmes/terminateActivity"
    headers = {'Content-Type': 'application/json'}
    jobIds = json.loads(listOfJobsId)
    result = post(url, headers, data=jobIds)
    return result


def getActivityReport(listOfJobsId):
    url = serviceUrl + "pmes/getActivityReport"
    headers = {'Content-Type': 'application/json'}
    jobId = json.loads(listOfJobsId)
    print(jobId)
    result = post(url, headers, data=jobId)
    return result


def createActivity(data):
    url = serviceUrl + "pmes/createActivity"
    headers = {'Content-Type': 'application/json'}
    #listJobsDef = json.dumps(dictData)
    result = post(url, headers, data=data)
    return result


def main(args):
    action = args[1]
    if action == "getSystemStatus":
        r = getSystemStatus()
        print(r)
    elif action == "terminateActivity":
        r = terminateActivity('["18045e7f-a670-46fe-a067-3b1a19870bcf"]')
        print(r)
    elif action == "getActivityReport":
        r = getActivityReport('["18045e7f-a670-46fe-a067-3b1a19870bcf"]')
        print(r)
    elif action == "createActivity":
        jsonFile = args[2]
        with open(jsonFile, 'r') as json_data:
            content = json_data.read()
            data = json.loads(content)
        print(data)
        r = createActivity(data)
        print(r)
    else:
        print("Action {} not implemented".format(action))


if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("Usage: action")
        print("action: getSystemStatus | terminateActivity | getActivityReport | createActivity")
    else:
        main(sys.argv)
