
The demo application consists of a client GUI app and an RMI server. The RMI server is essentially a simple "Lock Server"... it has a method to acquire an exclusive lock ("acquireLock"), and a method to release the lock ("releaseLock"). If another client already has the exclusive lock, the #acquireLock method will block until the lock becomes available. The client GUI allows the user to create multiple lock clients which can then compete to acquire the lock. The "cancel" function is essentially the purpose of the Interruptible RMI library - pressing "cancel" interrupts the client thread's blocking RMI call, and returns control back to that thread.

You can start the demo by running the executable jar, e.g.

 java -jar interruptiblermi-demo.jar

Note that this app requires JDK5 or greater.