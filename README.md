## Interruptible RMI


## From the package javadoc
This package provides a mechanism for interrupting RMI calls. An RMISocketFactory implementation is provided ([`InterruptibleRMISocketFactory`](../../../../org/neilja/net/interruptiblermi/InterruptibleRMISocketFactory.html "class in org.neilja.net.interruptiblermi")), as well as a ThreadFactory ([`InterruptibleRMIThreadFactory`](../../../../org/neilja/net/interruptiblermi/InterruptibleRMIThreadFactory.html "class in org.neilja.net.interruptiblermi")). Install the RMISocketFactory in the standard way. This can be done by the RMI server binding the factory when calling `UnicastRemoteObject.exportObject(Remote, int, RMIClientSocketFactory, RMIServerSocketFactory)` or by the client calling `RMISocketFactory.setSocketFactory(RMISocketFactory)`.

### Client Side

From the client, use a thread from `InterruptibleRMIThreadFactory` to make RMI calls. Invoking `#interrupt()` on that thread will result in the RMI IO operation being terminated and the IO method returning.

Typically a blocking IO call cannot be interrupted, but this limitation can be circumvented by directly closing the IO object (i.e. the socket). An RMI client socket object returned by `InterruptibleRMISocketFactory` registers with the thread instance returned by `InterruptibleRMIThreadFactory` when the socket is about to enter a blocking IO operation (and unregisters on exit of that IO operation). So, when `#interrupt()` is invoked, the thread has a reference to the socket object that is currently in blocking IO. The thread directly calls `#close()` on that socket (after sending a shutdown signal to the RMI server), terminating the blocking IO operation, and effectively simulating a regular interrupt. Note that after the interrupt, the socket is now dead, and the thread's interrupt status has been set.

### Server Side

When the client interrupts the RMI call, the server RMI thread that was spawned in response to that RMI call is typically still alive. If the server RMI thread had been waiting for a resource, e.g. waiting for a lock on synchronized object such as a database row, then the thread would not know that the client had interupted the RMI call, and thus the "zombie" thread could acquire the contested resource, thus possibly denying a healthy thread access to the resource. To combat this situation, a mapping is maintained between server RMI threads and the RMI socket that spawned that thread. If the `#close` method is invoked on the server RMI socket, `Thread#interrupt` is called on on the zombie thread. At this point, or at any point that the server RMI thread can potentially acquire a contested resource, the "zombie" status of the thread can be tested using `InterruptibleRMISocketFactory.isCurrentRMIServerThreadSocketAlive(). If this method returns true, the zombie RMI server thread should attempt to die (either by returning immediately or throwing an exception as appropriate to your application).

The code snippet below shows how the server RMI thread can be a good citizen:

```java
     while (isContestedResourceAvailable == false)
     {
            this.wait();
     
            // on wakeup
            if (InterruptibleRMISocketFactory.isCurrentRMIServerThreadSocketAlive() == false)
            {
                    // this thread is a "zombie" thread - time to die!
                    throw new RuntimeException("The RMI socket associated with this thread is not alive");
            }
     
            // otherwise, do something fun with the acquired resource...
     }
```

However, note that closing of the RMI socket by the client does not necessarily result in the server RMI thread knowing that the client has closed its end of the socket (at least this is the case with the Sun JDK 5.0 implementation). That is, calling `myServerRMISocket.isClosed()` doesn't necessarily return true even after the client has interrupted the RMI call and invoked `myClientRMISocket.close()`. To signal that the client has shutdown the socket, the `#interrupt()` method on thread instances returned by `InterruptibleRMIThreadFactory` writes a shutdown signal (a byte value) to the client RMI socket's output stream before closing the socket. So when the zombie RMI server thread invokes `InterruptibleRMISocketFactory#isCurrentRMIServerThreadSocketAlive()`, that method checks if this byte value has been written to the thread's RMI socket, and can thus determine that the RMI call has indeed been interrupted.

Note that the `InterruptibleRMISocketFactory.isCurrentRMIServerThreadSocketAlive()` method only gives useful results if called from an RMI server thread. If called from a non-RMI server thread, this method will always return `false`. Therefore, the `InterruptibleRMISocketFactory.isCurrentThreadRMIServer()`method is provided to determine whether the current thread is an RMI server thread (i.e. has been associated with an RMI server socket). This can be useful for applications where the same code may be executed in response to client requests or as server-side maintenance tasks.

Therefore, the previous sample code could be re-written as follows to apply to general server threads:

     while (isContestedResourceAvailable == false)
     {
            this.wait();
     
            // on wakeup
            if (InterruptibleRMISocketFactory.isCurrentThreadRMIServer() == true &&
                InterruptibleRMISocketFactory.isCurrentRMIServerThreadSocketAlive() == false)
            {
                    // this thread is a "zombie" thread - time to die!
                    throw new RuntimeException("The RMI socket associated with this thread is not alive");
            }
     
            // otherwise, do something fun with the acquired resource...
     }
     



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
