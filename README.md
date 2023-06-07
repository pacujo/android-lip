An Android IRC Client
=====================

This is a proof of concept I have put together to explore the Jetpack
Compose framework and Android app development. While educational and
usable to an extent, it can't serve as a serious IRC client for two
reasons:

1. During idle times, Android dozes off placing apps and subsystems in
a deep sleep to save battery life. Thus, the IRC client will not be
able to react to PING requests from the server nor will it be able to
send PING requests of its own.

2. The IRC protocol ties the session to a TCP connection. TCP is not
multihomed. So when the phone roams between networks, the connection
is cut off. Furthermore, the server doesn't get an indication of a
dropped connection before it tries to talk to the client again (say,
with a PING). During those minutes, a new login with the same nick is
not possible as the old login is till valid.

Thus, IRC would require a cloud component that proxied for the client,
and the protocol between the cloud component and the client would need
to be something else than IRC. Preferably, the cloud component would
buffer conversations so they can be retrieved hours or days later when
the phone app reconnects.
