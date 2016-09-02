#
# A simple makefile for compiling three java classes
#

# define a makefile variable for the java compiler
#
JCC = javac

# define a makefile variable for compilation flags
# the -g flag compiles with debugging information
#
JFLAGS = -g

# typing 'make' will invoke the first target entry in the makefile 
# (the default one in this case)
#
default: Helper.class Receiver.class Sender.class SenderTimeout.class SendingThread.class TCPPacket.class AcknowledgeThread.class SenderReceiveThread.class

Helper.class: Helper.java
	$(JCC) $(JFLAGS) Helper.java

Receiver.class: Receiver.java
	$(JCC) $(JFLAGS) Receiver.java

Sender.class: Sender.java
	$(JCC) $(JFLAGS) Sender.java

SenderTimeout.class: SenderTimeout.java
	$(JCC) $(JFLAGS) SenderTimeout.java

SendingThread.class: SendingThread.java
	$(JCC) $(JFLAGS) SendingThread.java

TCPPacket.class: TCPPacket.java
	$(JCC) $(JFLAGS) TCPPacket.java

AcknowledgeThread.class: AcknowledgeThread.java
	$(JCC) $(JFLAGS) AcknowledgeThread.java

SenderReceiveThread.class: SenderReceiveThread.java
	$(JCC) $(JFLAGS) SenderReceiveThread.java

LogWrittingThread.class: LogWrittingThread.java
	$(JCC) $(JFLAGS) LogWrittingThread.java

# To start over from scratch, type 'make clean'.  
# Removes all .class files, so that the next make rebuilds them
#
clean: 
	$(RM) *.class