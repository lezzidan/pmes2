import requests
import json
import sys

ip = "localhost"
port = "8080"
serviceName = "trunk_war_exploded"
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


def createActivity(data):
    url = serviceUrl + "pmes/createActivity"
    headers = {'Content-Type': 'application/json'}
    listJobsDef = json.dumps(data)
    result = post(url, headers, data=listJobsDef)
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
    jobIds = json.loads(listOfJobsId)
    result = post(url, headers, data=jobIds)
    return result


def main(args):
    action = args[1]
    if action == "getSystemStatus":
        r = getSystemStatus()
        print(r)
    elif action == "terminateActivity":
        jobIds = []
        for i in xrange(2, len(args) - 1):
            jobIds.append(args[i])
        r = terminateActivity(str(jobIds))
        # r = terminateActivity('["18045e7f-a670-46fe-a067-3b1a19870bcf"]')
        print(r)
    elif action == "getActivityReport":
        jobIds = []
        for i in xrange(2, len(args) - 1):
            jobIds.append(args[i])
        r = getActivityReport(str(jobIds))
        # r = getActivityReport('["18045e7f-a670-46fe-a067-3b1a19870bcf"]')
        print(r)
    elif action == "createActivity":
        jsonFile = args[2]
        with open(jsonFile) as json_data:
            data = json.loads(json_data)
        r = createActivity(data)
        print(r)
    else:
        print "Action {} not implemented".format(action)


if __name__ == '__main__':
    main(sys.argv)
