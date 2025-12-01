# Makefile cheat sheet:
# https://www.gnu.org/software/make/manual/html_node/Automatic-Variables.html

DISC_LABEL		:= HenLoaderNext

MAKEFILE_DIR 	:= $(dir $(realpath $(lastword $(MAKEFILE_LIST))))
BDJSDK_HOME  	?= $(MAKEFILE_DIR)/../../../
BDSIGNER     	:= $(BDJSDK_HOME)/host/bin/bdsigner
MAKEFS       	:= $(BDJSDK_HOME)/host/bin/makefs
JAVA8_HOME    	?= $(BDJSDK_HOME)/host/jdk8
JAVA8 			:= $(JAVA8_HOME)/bin/java
JAVAC        	:= $(JAVA8_HOME)/bin/javac
JAR          	:= $(JAVA8_HOME)/bin/jar

export JAVA8_HOME

all: $(DISC_LABEL).iso

CLASSPATH		:= $(BDJSDK_HOME)/target/lib/interactive-stubs.zip:$(BDJSDK_HOME)/target/lib/bdjstack.jar:$(BDJSDK_HOME)/target/lib/rt.jar:src
JFLAGS        	:= -Xlint:-options -source 1.4 -target 1.4

LOADER_DSTDIR	:= build/henloader
LOADER_BD_PERM	:= org/bdj/bluray.InitXlet.perm
SOURCES	:= $(wildcard src/org/bdj/*.java) $(wildcard src/org/bdj/sandbox/*.java) $(wildcard src/org/bdj/api/*.java)

# Assemble the Blu-ray disc

DISC      	:= build/disc
BD_JAR    	:= $(DISC)/BDMV/JAR/00000.jar
BD_HEN    	:= $(DISC)/BDMV/AUXDATA/aiofix_USBpayload.elf
BD_META  	:= $(DISC)/BDMV/META/DL/bdmt_eng.xml
BD_BANNER 	:= $(DISC)/BDMV/META/DL/logo.png
AVCHD		:= $(BDJSDK_HOME)/resources/AVCHD

TMPL_DIRS  := $(shell find $(AVCHD)/ -type d)
TMPL_FILES := $(shell find $(AVCHD)/ -type f)

DISC_DIRS  := $(patsubst $(AVCHD)%,$(DISC)%,$(TMPL_DIRS))
DISC_FILES := $(patsubst $(AVCHD)%,$(DISC)%,$(TMPL_FILES)) \
			 $(BD_JAR) $(BD_META) $(BD_BANNER) $(BD_HEN)

$(DISC):
	mkdir -p $(dir $(DISC_FILES)) $(LOADER_DSTDIR)

$(DISC)/%: build/disc
		cp $(AVCHD)/$* $@

$(BD_JAR): $(DISC) $(SOURCES)
# 	mkdir -p $(LOADER_DSTDIR)
	$(JAVAC) -d $(LOADER_DSTDIR) -sourcepath src $(JFLAGS) -cp $(CLASSPATH) $(SOURCES)
	$(JAR) cf $@ -C $(LOADER_DSTDIR) .
	$(BDSIGNER) -keystore $(BDJSDK_HOME)/resources/sig.ks $@

$(BD_HEN): bd-metadata/aiofix_USBpayload.elf $(DISC)
	cp $< $@

# Metadata about the disc, including user-visible name and banner
$(BD_META): bd-metadata/bdmt_eng.xml $(DISC)
	cp $< $@
$(BD_BANNER): bd-metadata/logo.png $(DISC)
	cp $< $@

# Generate the final ISO containing HenLoader
$(DISC_LABEL).iso: $(MAKEFS) $(DISC_FILES)
	$(MAKEFS) -m 16m -t udf -o T=bdre,v=2.50,L=$(DISC_LABEL) $@ $(DISC)

# Cleaning just means deleting the build directory
.PHONY: clean
clean:
	rm -rf META-INF build $(DISC_LABEL).iso src/org/bdj/*.class src/org/bdj/sandbox/*.class src/org/bdj/api/*.class
