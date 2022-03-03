install:
	cp out/artifacts/Ce0_main_jar/Ce0.main.jar /usr/local/bin/Ce0.jar
	#cp slf4j-nop-2.0.0-alpha1.jar /usr/local/bin
	cp ce0.sh /usr/local/bin/ce0
	#cp freechains-host.sh         /usr/local/bin/freechains-host
	#cp freechains-sync.sh         /usr/local/bin/freechains-sync
	ls -l /usr/local/bin/[Cc]e*
	#freechains --version

test:
	echo "output std ()" > /tmp/tst.ce
	ce0 /tmp/tst.ce
