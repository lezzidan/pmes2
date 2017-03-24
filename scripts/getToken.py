#!/bin/bash
import subprocess
import json
import os


def parser(data):
    with open(data, 'r') as f:
        jsonStr = f.read()
    jsonData = json.loads(jsonStr)
    return jsonData['access']['token']['id']


def executeCommand(script):
    my_env = os.environ.copy()
    exCode = subprocess.call("./getToken.sh", shell=True, env=my_env)
    print(exCode)


def main():
    executeCommand('getToken.sh')
    token = parser('data.json')
    print(token)


if __name__ == '__main__':
    main()
