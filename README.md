Quick guide to create cerftificates using Java 11 keytool.

Example:

mkdir ssldemo and cd ssldemo

Generate the Client and Server Keystores

$ keytool -genkeypair -alias plainserverkeys -keyalg RSA -dname "CN=PlainServer,OU=PADA,O=CS,L=ATHENS,S=ATTICA,C=GR" 	-keypass mypass -keystore plainserver.jks -storepass mypass

$ keytool -genkeypair -alias plainclientkeys -keyalg RSA -dname "CN=PlainServer,OU=PADA,O=CS,L=ATHENS,S=ATTICA,C=GR" 	-keypass mypass -keystore plainclient.jks -storepass mypass

Export the server public certificate and create a seperate keystore

$ keytool -exportcert -alias plainserverkeys -file serverpub.cer -keystore plainserver.jks -storepass mypass

Certificate stored in file <serverpub.cer>

$ keytool -importcert -keystore serverpub.jks -alias serverpub -file serverpub.cer	     -storepass mypass

Trust this certificate? [no]: yes
Certificate was added to keystore

Export the client public certificate and create a seperate keystore

$ keytool -exportcert -alias plainclientkeys -file clientpub.cer -keystore plainclient.jks -storepass mypass

Certificate stored in file <clientpub.cer>

$ keytool -importcert -keystore clientpub.jks -alias clientpub -file clientpub.cer -storepass mypass

Trust this certificate? [no]: yes
Certificate was added to keystore



$ ls
clientpub.cer clientpub.jks plainclient.jks plainserver.jks serverpub.cer serverpub.jks


Remember: put keytool.exe to your path directory so that your cmd may recognise the keytool commands.
