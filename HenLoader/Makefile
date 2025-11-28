# Makefile cheat sheet:
# https://www.gnu.org/software/make/manual/html_node/Automatic-Variables.html

all: build/henloader.iso

# Known-good JDK packages for Linux
# Source: https://github.com/adoptium/temurin8-binaries/releases

ARCH := $(shell uname -m)
ifeq ($(ARCH),aarch64)
	JDK8_PACKAGE  := thirdparty/OpenJDK8U-jdk_aarch64_linux_hotspot_8u462b08.tar.gz
	JDK11_PACKAGE := thirdparty/OpenJDK11U-jdk_aarch64_linux_hotspot_11.0.28_6.tar.gz
else ifeq ($(ARCH),x86_64)
	JDK8_PACKAGE  := thirdparty/OpenJDK8U-jdk_x64_linux_hotspot_8u462b08.tar.gz
	JDK11_PACKAGE := thirdparty/OpenJDK11U-jdk_x64_linux_hotspot_11.0.28_6.tar.gz
else
	$(error Unknown ARCH "$(ARCH)")
endif

# JDK8 is required for almost every part of the process

JDK8  := build/jdk8
JAVA8 := $(JDK8)/bin/java
$(JAVA8):
	mkdir -p $(JDK8)
	tar -xf $(JDK8_PACKAGE) -C $(JDK8) --strip-components=1

# A Linux port of NetBSD makefs is used to create the final UDF-format ISO image

MAKEFS := build/makefs
MAKEFS_SOURCES := $(wildcard thirdparty/makefs/* thirdparty/makefs/udf/*)
$(MAKEFS): $(MAKEFS_SOURCES)
	$(MAKE) -C thirdparty/makefs
	mkdir -p $(dir $(MAKEFS))
	mv thirdparty/makefs/makefs $(MAKEFS)

# BD-J JAR files need to be signed with BDSigner

BDTOOLS  := thirdparty/bd-tools
SECCPATH := $(BDTOOLS)/security.jar:$(BDTOOLS)/bcprov-jdk15-137.jar:$(BDTOOLS)/jdktools.jar
KEYSTORE := thirdparty/bd-certificates/keystore.store
BDSIGNER := $(JAVA8) -cp $(SECCPATH) net.java.bd.tools.security.BDSigner -keystore $(KEYSTORE)

# InitXlet is the initial Xlet that the PS4 loads. It always lives on the Blu-ray disc.

CPATH  := thirdparty/bd-stubs/interactive.zip:thirdparty/topsecret/rt.jar:thirdparty/topsecret/bdjstack.jar:src
JFLAGS := -Xlint:all -Xlint:-static -Xlint:-serial -Xlint:-options -source 1.3 -target 1.3

LOADER_DSTDIR  := build/henloader
LOADER_BD_PERM := org/bdj/bluray.InitXlet.perm
LOADER_SOURCES += org/bdj/sandbox/DisableSecurityManagerAction.java
LOADER_SOURCES += org/bdj/external/BinLoader.java
LOADER_SOURCES += org/bdj/external/Helper.java
LOADER_SOURCES += org/bdj/external/Kernel.java
LOADER_SOURCES += org/bdj/external/KernelOffset.java
LOADER_SOURCES += org/bdj/external/Poops.java
LOADER_SOURCES += org/bdj/external/Lapse.java
LOADER_SOURCES += org/bdj/InitXlet.java
LOADER_SOURCES += org/bdj/MessagesOutputStream.java
LOADER_SOURCES += org/bdj/Screen.java

build/henloader.jar: $(JAVA8) $(addprefix src/,$(LOADER_SOURCES)) src/$(LOADER_BD_PERM)
	mkdir -p $(LOADER_DSTDIR)
	mkdir -p $(LOADER_DSTDIR)/$(dir $(LOADER_BD_PERM))
	cp src/$(LOADER_BD_PERM) $(LOADER_DSTDIR)/$(LOADER_BD_PERM)
	mkdir -p $(LOADER_DSTDIR)/org/homebrew/
	$(JDK8)/bin/javac -d $(LOADER_DSTDIR) -sourcepath src $(JFLAGS) -cp $(CPATH) $(addprefix src/,$(LOADER_SOURCES))
	$(JDK8)/bin/jar cf $@ -C $(LOADER_DSTDIR) .
	$(BDSIGNER) $@
	-rm META-INF/SIG-BD00.RSA
	-rm META-INF/SIG-BD00.SF
	-rmdir META-INF

# Assemble the Blu-ray disc

DISC      := build/disc
BD_JO     := $(DISC)/BDMV/BDJO/00000.bdjo
BD_JAR    := $(DISC)/BDMV/JAR/00000.jar
BD_FONT   := $(DISC)/BDMV/AUXDATA/00000.otf
BD_FNTIDX := $(DISC)/BDMV/AUXDATA/dvb.fontindex
BD_HEN    := $(DISC)/BDMV/AUXDATA/aiofix_USBpayload.elf
BD_META   := $(DISC)/BDMV/META/DL/bdmt_eng.xml
BD_BANNER := $(DISC)/BDMV/META/DL/logo.png
BD_INDEX  := $(DISC)/BDMV/index.bdmv
BD_MVOBJ  := $(DISC)/BDMV/MovieObject.bdmv
BD_ID     := $(DISC)/CERTIFICATE/id.bdmv
BD_ACRT   := $(DISC)/CERTIFICATE/app.discroot.crt
BD_BCRT   := $(DISC)/CERTIFICATE/bu.discroot.crt
BD_ALL    := $(BD_JO) $(BD_JAR) $(BD_FONT) $(BD_FNTIDX) $(BD_META) $(BD_BANNER) \
             $(BD_INDEX) $(BD_MVOBJ) $(BD_ID) $(BD_ACRT) $(BD_BCRT) $(BD_HEN)

# Create directories
$(DISC): $(sort $(dir $(BD_ALL)))
$(sort $(dir $(BD_ALL))):
	mkdir -p $(dir $(BD_ALL))

# bdjo.xml/00000.bdjo tells the Blu-ray player which Xlet subclass to load
$(BD_JO): bd-metadata/bdjo.xml $(DISC) $(JAVA8)
	$(JAVA8) -jar thirdparty/bd-tools/bdjo.jar $< $@

# Signed JAR containing the HenLoader Xlet
$(BD_JAR): build/henloader.jar $(DISC)
	cp $< $@

# There needs to be at least one font file on the disc, if I understand correctly
$(BD_FONT): bd-metadata/OpenSans-Regular.otf $(DISC)
	cp $< $@
$(BD_FNTIDX): bd-metadata/dvb.fontindex $(DISC)
	cp $< $@
$(BD_HEN): bd-metadata/aiofix_USBpayload.elf $(DISC)
	cp $< $@

# Metadata about the disc, including user-visible name and banner
$(BD_META): bd-metadata/bdmt_eng.xml $(DISC)
	cp $< $@
$(BD_BANNER): bd-metadata/logo.png $(DISC)
	cp $< $@

# Blu-ray index that points the player towards MovieObject
$(BD_INDEX): bd-metadata/index.xml $(DISC) $(JAVA8)
	$(JAVA8) -jar thirdparty/bd-tools/index.jar $< $@

# Blu-ray movie object that somehow tells the player to run the BD-J xlet? Extremely cursed
$(BD_MVOBJ): bd-metadata/movieobject.xml $(DISC) $(JAVA8)
	$(JAVA8) -jar thirdparty/bd-tools/movieobject.jar $< $@

# Just an orgId really, needs to match bdjo and perm
$(BD_ID): bd-metadata/id.xml $(DISC) $(JAVA8)
	$(JAVA8) -jar thirdparty/bd-tools/id.jar $< $@

# Certificates are taken from BDJ-SDK, need to match the keystore
$(BD_ACRT): thirdparty/bd-certificates/app.discroot.crt $(DISC)
	cp $< $@
$(BD_BCRT): thirdparty/bd-certificates/bu.discroot.crt $(DISC)
	cp $< $@

# Generate the final ISO containing HenLoader

DISC_LABEL := HenLoader

build/henloader.iso: $(MAKEFS) $(BD_ALL)
	$(MAKEFS) -m 16m -t udf -o T=bdre,v=2.50,L=$(DISC_LABEL) $@ $(DISC)

# Cleaning just means deleting the build directory

.PHONY: clean
clean:
	rm -r build
