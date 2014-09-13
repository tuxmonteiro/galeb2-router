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
  ROUTE='127.0.0.1:9090'
fi

VHOST=$2
if [ "x$VHOST" == "x" ]; then
  VHOST='lol.localdomain'
fi

BACKEND1=$3
if [ "x$BACKEND1" == "x" ]; then
  BACKEND1_HOST='127.0.0.1'
  BACKEND1_PORT='8081'
else
  BACKEND1_HOST=${BACKEND1%%:*}
  BACKEND1_PORT=${BACKEND1##*:}
fi

BACKEND2=$4
if [ "x$BACKEND2" == "x" ]; then
  BACKEND2_HOST='127.0.0.1'
  BACKEND2_PORT='8082'
else
  BACKEND2_HOST=${BACKEND2%%:*}
  BACKEND2_PORT=${BACKEND2##*:}
fi

LOADBALANCE=$5
if [ "x$LOADBALANCE" == "x" ]; then
  LOADBALANCE="RandomPolicy"
fi

curl -XPOST "http://$ROUTE/virtualhost" -d '
{
    "version": 28031976,
    "routes":[
    {
      "id": "'$VHOST'",
      "properties": {
        "loadBalancePolicy": "'$LOADBALANCE'"
      },
      "backends": []
    }
    ]
}'

curl -XPOST "http://$ROUTE/backend" -d '
{
    "version": 28031977,
    "routes":[
    {
      "id": "'$VHOST'",
      "backends": [
        {
            "id":"'$BACKEND1_HOST:$BACKEND1_PORT'"
        },
        {
            "id":"'$BACKEND2_HOST:$BACKEND2_PORT'"
        }
        ]
    }
    ]
}'

# Examples:
#curl -XPOST "http://127.0.0.1:9090/virtualhost" -d '{"version": 1, routes:[{"id": "lol.localdomain", "backends":[]}]}'
#curl -XPOST "http://127.0.0.1:9090/backend" -d '{"version": 2, routes:[{"id": "lol.localdomain", "backends":[{"id":"127.0.0.1:8082"}]}]}'

