#!/bin/bash
mvn exec:java "-Dexec.mainClass=ch.adjudicator.agent.SmartAgent" "-Dexec.args=--key ${CHESS_KEY} --name KalkuttakutteV102 --mode RANKED --time 300+0"
