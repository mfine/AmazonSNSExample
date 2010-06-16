AmazonSNSExample contains a simple example sender and receiver, along with the dependent libraries from AWS, Jetty, and Jackson.

To compile the files, run:

   > ant build

To start a sender, run:

   > ant sender

To start a receiver, run:

   > ant receiver

To start a receiver on port 9000, run:

   > ant receiver -Dargs=9000
