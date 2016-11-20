#!/bin/sh
######################################################################
# ident @(#) $Id: startProxy
# - DME2 startproxy
# Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
######################################################################
[ "${DME2PROXY_SCRIPT_DEBUG:-}x" != "x" ] && SCRIPT_DEBUG="set -x"
${SCRIPT_DEBUG:-}

ARGS="$@"

PATH="/usr/bin${PATH:+:}${PATH:-}"; export PATH

umask 022

if [ "${DME2_HOME:-}x" = "x" ]; then
    # This script is assumed to be located in ${DME2_HOME}/bin
	    DME2_HOME="`dirname $0`"; export DME2_HOME
		    DME2_HOME="`cd ${DME2_HOME}/..; pwd`"
fi

export DME2_HOME

CLASSPATH=${DME2_HOME}/lib/*:${CLASSPATH};export CLASSPATH
if [ "${JAVA_HOME}x" == "x" ]; then
	echo "JAVA_HOME is not set in the environment. Exiting."
	exit 1
elif [ ! -f "${JAVA_HOME}/bin/java" ]; then
	echo "${JAVA_HOME}/bin/java not found. Exiting."
	exit 1
fi

INS_CNT=`ps -ef | grep -v grep | grep 'dme2ProxyInst=1' | wc -l`;export INS_CNT
if [ "${INS_CNT}" == "1" ]; then
	    echo "DME2IngressProxy instance already running, pid details shown below. Exiting launch of script."
		echo "####################################################################"
		ps -ef | grep -v grep | grep 'dme2ProxyInst=1'
		echo "####################################################################"

	    exit 1
fi

nohup ${JAVA_HOME}/bin/java -Ddme2ProxyInst=1 -DAFT_ENVIRONMENT=AFTUAT -DAFT_LATITUDE=22 -DAFT_LONGITUDE=33.6 -Djava.util.logging.config.file=${PROXY_ETC_DIR}logging.properties com.att.aft.dme2.api.proxy.DME2IngressProxy $ARGS > ${PROXY_LOGS_DIR}proxyServer.out 2>&1 &
