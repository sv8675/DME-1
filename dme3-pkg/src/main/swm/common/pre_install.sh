#!/bin/sh -x
##############################################################################
# - AFT SWM
# - Copyright 2016 AT&T Intellectual Properties
##############################################################################
set -ax

SWM_PATH=${INSTALL_ROOT}/opt/app/aft/aftswmnode
SWM_SECURE_PATH=/usr/localcw/opt/aftswmnode
ROOT_DIR=${INSTALL_ROOT}/opt/app/aft/aftswmnode
LINK_DIR=${INSTALL_ROOT}/opt/app/aft/aftswmnode/bin/internal
START_SCRIPT=${ROOT_DIR}/bin/swmnoded
BOOTSTRAP_FILE=${ROOT_DIR}/etc/swm-bootstrap.properties
LOCAL_BOOTSTRAP_FILE=${ROOT_DIR}/etc/swm-bootstrap-local.properties
AFT_PROP_FILE=${ROOT_DIR}/etc/aft.properties
SWM_USER=aft
SWM_GROUP=aft
SUDO_ROOT=/usr/localcw/opt/sudo
SWM_SUDO_FILE=/usr/localcw/opt/sudo/sudoers.d/0099_swm

cd `dirname $0` || exit 1

chmod 755 *

if [ "${AFTSWM_ACTION_COMPONENT}" = "com.att.aft.swm:swm-node" ]; then
	if [ ! -f ${SWM_PATH}/bin/internal.home.env ]; then
	   echo "AFTSWM_NODE_HOME=${SWM_PATH}" > ${SWM_PATH}/bin/internal/home.env
	fi 
	
	# make a copy of lib 
	cp -rf ${SWM_PATH}/lib ${SWM_PATH}/stage/.tmp/lib || exit 2
	
	# make sure the bin/internal/pkgziptool script references the lib copy
	grep AFTSWM_ACTION_COMPONENT ${SWM_PATH}/bin/internal/pkgziptool
	if [ $? -ne 0 ]; then
		# if not, copy a temporary one over
		cp -f pkgziptool.tmp ${SWM_PATH}/bin/internal/pkgziptool || exit 3
		chmod 755 ${SWM_PATH}/bin/internal/pkgziptool || exit 4
	fi
fi

OS=`uname`
if [ "${OS}" = "SunOS" ]; then
    CURRENT_USER=`/usr/xpg4/bin/id -un`
else
    CURRENT_USER=`id -un`
fi
export CURRENT_USER

if [ "${CURRENT_USER}" = root ]; then
        SU_CMD="su ${SWM_USER}"
        ./setupsudo.sh || exit 4
fi

exit 0
