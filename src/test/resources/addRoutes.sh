#!/bin/bash

# Copyright (c) 2014 Globo.com - ATeam
# * All rights reserved.
# *
# * This source is subject to the Apache License, Version 2.0.
# * Please see the LICENSE file for more information.
# *
# * Authors: See AUTHORS file
# *
# * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY
# * KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
# * IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A
# * PARTICULAR PURPOSE.

ROUTE=$1
if [ "x$ROUTE" == "x" ]; then
  ROUTE='127.0.0.1:9000'
fi

VHOST=$2
if [ "x$VHOST" == "x" ]; then
  VHOST='lol.localdomain'
fi

BEPOOL=$3
if [ "x$BEPOOL" == "x" ]; then
  BEPOOL='pool0'
fi

BACKEND1=$4
if [ "x$BACKEND1" == "x" ]; then
  BACKEND1_HOST='127.0.0.1'
  BACKEND1_PORT='8081'
else
  BACKEND1_HOST=${BACKEND1%%:*}
  BACKEND1_PORT=${BACKEND1##*:}
fi

BACKEND2=$5
if [ "x$BACKEND2" == "x" ]; then
  BACKEND2_HOST='127.0.0.1'
  BACKEND2_PORT='8082'
else
  BACKEND2_HOST=${BACKEND2%%:*}
  BACKEND2_PORT=${BACKEND2##*:}
fi

LOADBALANCE=$6
if [ "x$LOADBALANCE" == "x" ]; then
  LOADBALANCE="HashPolicy"
fi

RULE=$7
if [ "x$RULE" == "x" ]; then
  RULE='rule0'
fi

curl -XPOST "http://$ROUTE/backendpool" -d '
{
    "version": 28031976,
    "id": "'$BEPOOL'",
    "properties": {
        "loadBalancePolicy": "'$LOADBALANCE'"
      }
}'

curl -XPOST "http://$ROUTE/backend" -d '
{
    "version": 28031977,
    "id":"'$BACKEND1_HOST:$BACKEND1_PORT'",
    "parentId": "'$BEPOOL'"
}'

curl -XPOST "http://$ROUTE/backend" -d '
{
    "version": 28031978,
    "id":"'$BACKEND2_HOST:$BACKEND2_PORT'",
    "parentId": "'$BEPOOL'"
}'

curl -XPOST "http://$ROUTE/virtualhost" -d '
{
    "version": 28031979,
    "id": "'$VHOST'",
    "properties": { }
}'

curl -XPOST "http://$ROUTE/rule" -d '
{
    "version": 28031980,
    "id": "'$RULE'",
    "properties": {
    "parentId": "'$VHOST'",
    "properties": {
        "ruleType": "UriPath",
        "returnType": "BackendPool",
        "orderNum": 1,
        "match": "/",
        "returnId": "'$BEPOOL'",
        "default": false
    }
}'


# Examples:
#curl -XPOST "http://127.0.0.1:9090/backendpool" -d '{"version": 1, "id": "pool0", "properties": {}}'
#curl -XPOST "http://127.0.0.1:9090/virtualhost" -d '{"version": 2, "id": "lol.localdomain", "properties": {}}'
#curl -XPOST "http://127.0.0.1:9090/backend" -d '{"version": 3, "id": "127.0.0.1:8081", "parentId": "pool0"}'
#curl -XPOST "http://127.0.0.1:9090/backend" -d '{"version": 4, "id": "127.0.0.1:8082", "parentId": "pool0"}'
#curl -XPOST "http://127.0.0.1:9090/rule" -d '{"version": 5, "id": "rule0", "parentId": "lol.localdomain", "properties": { "ruleType":"UriPath","returnType":"BackendPool","orderNum":1,"match":"/","returnId":"pool0","default": false} }'

