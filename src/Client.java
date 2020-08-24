import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Client implements Runnable {
    ReentrantLock lock;
    Condition condition;
    Socket clientSocket = null;
    int clientID;
    int portNumber;
    int serverPortNumber;
    int processOrder;
    int requestedTicket;
    ObjectOutputStream oos;
    ObjectInputStream ois;
    Message receivedMsg = null;

    public Client(ReentrantLock lock, Condition condition, int clientID, int portNumber, int serverPortNumber, int requestedTicket, int processOrder) {
        this.lock = lock;
        this.condition = condition;
        this.clientID = clientID;
        this.portNumber = portNumber;
        this.serverPortNumber = serverPortNumber;
        this.processOrder = processOrder;
        this.requestedTicket = requestedTicket;
    }

    private void WriterThread() {
        lock.lock();
        try {
            if (!lock.isHeldByCurrentThread()) {
                condition.await();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            System.out.println("Time: " + java.time.LocalTime.now());
            System.out.println("Writer" + clientID + "   " + (requestedTicket + 1) + " numaralı bileti alabilmek için istekte bulunuyor.....");
            clientSocket = new Socket("localhost", serverPortNumber);
            Message requestedMsg = new Message();
            requestedMsg.setSenderID(clientID);
            requestedMsg.setSenderPortNumber(portNumber);
            requestedMsg.setContent(requestedTicket);
            requestedMsg.setType(Message.Type.SERVICE_MAKE_REZERVATION);
            oos = new ObjectOutputStream(clientSocket.getOutputStream());
            oos.writeObject(requestedMsg);
            ois = new ObjectInputStream(clientSocket.getInputStream());
            receivedMsg = (Message) ois.readObject();
            System.out.println("Writer" + clientID + " Sunucudan " + receivedMsg.getType() + " tipinde bir mesaj içeriği aldı : mesaj --> " + (String) receivedMsg.getContent());
            System.out.println("--------------------------------------------------");
            condition.signalAll();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    private void CancelThread() {
        lock.lock();
        try {
            if (!lock.isHeldByCurrentThread()) {
                condition.await();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            System.out.println("Time: " + java.time.LocalTime.now());
            System.out.println("CancelWriter" + clientID + "   " + (requestedTicket + 1) + " numaralı biletini iptal etmek için istekte bulunuyor...");
            clientSocket = new Socket("localhost", serverPortNumber);
            Message requestedMsg = new Message();
            requestedMsg.setSenderID(clientID);
            requestedMsg.setSenderPortNumber(portNumber);
            requestedMsg.setContent(requestedTicket);
            requestedMsg.setType(Message.Type.SERVICE_CANCEL_REZERVATION);
            oos = new ObjectOutputStream(clientSocket.getOutputStream());
            oos.writeObject(requestedMsg);

            ois = new ObjectInputStream(clientSocket.getInputStream());
            receivedMsg = (Message) ois.readObject();
            System.out.println("CancelWriter" + clientID + " Sunucudan " + receivedMsg.getType() + " tipinde bir mesaj içeriği aldı : mesaj --> " + (String) receivedMsg.getContent());
            System.out.println("--------------------------------------------------");
            condition.signalAll();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    private void ReaderThread() {
        lock.lock();
        try {
            if (!lock.isHeldByCurrentThread()) {
                condition.await();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            System.out.println("Time: " + java.time.LocalTime.now());
            System.out.println("Reader" + clientID + " bilet alabilmek için bilet listesini okuma isteğinde bulunuyor...");
            clientSocket = new Socket("localhost", serverPortNumber);
            Message requestedMsg = new Message();
            requestedMsg.setSenderID(clientID);
            requestedMsg.setSenderPortNumber(portNumber);
            requestedMsg.setType(Message.Type.SERVICE_READ_REZERVATION_LIST);
            oos = new ObjectOutputStream(clientSocket.getOutputStream());
            oos.writeObject(requestedMsg);
            ois = new ObjectInputStream(clientSocket.getInputStream());
            receivedMsg = (Message) ois.readObject();
            System.out.println("Reader" + clientID + " Sunucudan " + receivedMsg.getType() + " tipinde bir mesaj içeriği aldı.");
            Flight flight = (Flight) receivedMsg.getContent();
            System.out.println("*****Bilet Numaraları*****");
            for (int j = 0; j < flight.getTicketList().length; j++) {
                String ticketNumber = flight.getTicketList()[j].getTicketNumber();
                Boolean ticketState = flight.getTicketList()[j].isTicketState();

                if (ticketState == false) {
                    System.out.print(ticketNumber + " : Boş *--* ");
                } else {
                    System.out.print(ticketNumber + " : Dolu *--* ");
                }
            }
            System.out.println("\n--------------------------------------------------");
            condition.signalAll();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void run() {
        try {
            if (processOrder == 0) {

                ReaderThread();
                Thread.sleep(50);
                WriterThread();

            } else if (processOrder == 1) {

                WriterThread();
                Thread.sleep(100);
                ReaderThread();

            } else if (processOrder == 2) {

                WriterThread();
                Thread.sleep(50);
                CancelThread();
                Thread.sleep(50);
                ReaderThread();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

