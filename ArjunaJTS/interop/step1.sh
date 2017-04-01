#!/bin/bash
set -e

source init.sh

# patch and build WildFly 
function build_wf {
  # WildFly blocks the import of foreign transactions, until that policy is changed we need to patch it:
  if [ -d $WORKSPACE/narayana/jboss-as ]; then
    cp -r $WORKSPACE/narayana/jboss-as $QS_DIR/tmp
    cd $QS_DIR/tmp/jboss-as
  else
    # we need the source to be able to patch it
    echo "WARNING could not find wildfly 5_BRANCH cloning instead"
    git clone https://github.com/jbosstm/jboss-as.git $QS_DIR/tmp
    cd $QS_DIR/tmp/wildfly
    git checkout 5_BRANCH
  fi

  git apply $QS_DIR/interop.wildfly.diff

  ./build.sh clean install -DskipTests -Drelease=true -Dlicense.skipDownloadLicenses=true -Dversion.org.jboss.narayana=$NARAYANA_CURRENT_VERSION
}

# patch and build glassfish 
function build_gf {
  rm -rf $QS_DIR/tmp/glassfish
  svn checkout https://svn.java.net/svn/glassfish~svn/trunk/main $QS_DIR/tmp/glassfish  
  cd $QS_DIR/tmp/glassfish  
  patch -p0 -i $QS_DIR/GLASSFISH-21532.diff  
  mvn install -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -DskipTests  
}

# build the demo EJBs
function build_demo {
  cd $QS_DIR/src
  mvn clean install
}

# TODO rm -rf $QS_DIR/tmp
mkdir -p $QS_DIR/tmp

build_wf
#build_gf
build_demo

