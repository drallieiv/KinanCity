# Pallet Town email validation server

## What this module does

This module can be run as a standalone service and will :

- Start a **Mail server** listening to default port 25
- For each mailed received from niantic, the **activation link** is grabbed
- A web request is made to the **activation** link

## How to setup

The machine running the email server must be accessible from **the internet**.
If needed, configure any router/firewall/proxy to redirect traffic from **port 25** to the host running the server.

You must own a domain name and have access to DNS entries
add a **MX entry** redirecting emails to the server.
You may need to add a **A entry** to map a DNS to your machine IP address, as MX entries do not work directly with IP addresses.

## How to run

Grab the last compiled version from https://github.com/drallieiv/PalletTown/releases

or

Compile it yourself (see below)

Run with java :  
`java -jar target/PalletTown-mail-1.0-SNAPSHOT.jar`

## How to compile

- compile the application :  
`mvn compile`


## Notes

The server does not store received emails in any way. If the server is not running when the email is sent, or if it stopped before completing activation, then the activation link is lost forever.

As Mail Transfer Agents usually make a few retry, you could still start the server in time.

You may also grab the activation links manually from the application logs.
