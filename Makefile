install:
	cp out/artifacts/ceu_main_jar/ceu.main.jar /usr/local/bin/ceu.jar
	#cp slf4j-nop-2.0.0-alpha1.jar /usr/local/bin
	cp ceu.sh /usr/local/bin/ceu
	#cp freechains-host.sh         /usr/local/bin/freechains-host
	#cp freechains-sync.sh         /usr/local/bin/freechains-sync
	ls -l /usr/local/bin/[Cc]e*
	#freechains --version

test:
	echo "output std ()" > /tmp/tst.ce
	ceu /tmp/tst.ce
