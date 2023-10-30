import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CentralServer {
    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private BlockingQueue<Runnable> taskQueue;
    private AtomicInteger taskIdCounter = new AtomicInteger(0);

    public CentralServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        taskQueue = new LinkedBlockingQueue<>();
        executorService = new ThreadPoolExecutor(2, 10, 30, TimeUnit.SECONDS, taskQueue);
    }

    public void start() {
        System.out.println("Central Server started.");
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                executorService.execute(clientHandler);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        private int clientId;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            this.clientId = taskIdCounter.incrementAndGet();
        }

        @Override
        public void run() {
            try (
                DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())
            ) {
                // Implemente a lógica de autenticação aqui

                // Leia a tarefa do cliente
                byte[] task = readTaskFromClient(in);

                // Execute a tarefa e obtenha o resultado
                byte[] result = executeTask(task);

                // Envie o resultado de volta para o cliente
                sendResultToClient(result, out);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Implemente métodos para ler tarefas, executar tarefas e enviar resultados.

        private byte[] readTaskFromClient(DataInputStream in) throws IOException {
            int taskLength = in.readInt(); // Leitura do tamanho da tarefa
            byte[] task = new byte[taskLength];
            in.readFully(task); // Leitura dos dados da tarefa
            return task;
        }
    
        // Método para executar a tarefa (simplificado)
        private byte[] executeTask(byte[] task) {
            // Implemente a lógica de execução da tarefa, como o uso de JobFunction.execute(task)
            // ou qualquer lógica específica do seu projeto.
            // Aqui, estamos apenas simulando uma execução com a mesma tarefa.
            return task;
        }
    
        // Método para enviar o resultado de volta ao cliente
        private void sendResultToClient(byte[] result, DataOutputStream out) throws IOException {
            out.writeInt(result.length); // Envio do tamanho do resultado
            out.write(result); // Envio dos dados do resultado
        }
    }

    public static void main(String[] args) {
        try {
            CentralServer server = new CentralServer(8080);
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
