## Interruptible RMI


## Interruptible RMI Demo


The demo application consists of a client GUI app and an RMI server. The RMI server is essentially a simple "Lock Server"... 
it has a method to acquire an exclusive lock (`"#acquireLock"`), and a method to release the lock (`#releaseLock`). 
If another client already has the exclusive lock, the `#acquireLock` method will block until the lock becomes available.
The client GUI allows the user to create multiple lock clients which can then compete to acquire the lock.
The "cancel" function is essentially the purpose of the Interruptible RMI library - pressing "cancel" interrupts
the client thread's blocking RMI call, and returns control back to that thread.

You can start the demo by running the executable jar, i.e.
```
java -jar interruptiblermi-demo.jar
```

You can exit the client by closing the main window or via the File menu. Note that running the executable jar starts both the client 
and the remote server. When you exit the client, the remote server is still running. Type `exit` (and enter) in the shell
window to kill the server.

Note that this app requires JDK5 or greater.
