package kr.hs.dgsw.network.test01.n2118.server;

import kr.hs.dgsw.network.test01.n2118.Query;
import kr.hs.dgsw.network.test01.n2118.Result;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Base64;
import java.util.List;

public class ClientThread extends Thread {
    private final Socket client;
    private final BufferedReader reader;
    private final BufferedWriter writer;


    public ClientThread(Socket client) throws IOException {
        this.client = client;

        this.reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
        this.writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
    }

    @Override
    public void run() {
        try {
            while (true) {
                Query query = Query.receive(reader);
                if (!query.getCommand().equals("LOGIN")) continue;

                List<String> arguments = query.getArguments();

                String id = arguments.get(0);
                String pw = arguments.get(1);

                if (id.equals(Server.ID) && pw.equals(Server.PW)) break;

                String result = new Result("ERROR")
                        .addArgument("id or password is wrong")
                        .build();

                writer.append(result).flush();
            }

            {
                String result = new Result("SUCCESS").build();
                writer.append(result).flush();
            }

            mainLoop:
            while (true) {
                Query query = Query.receive(reader);

                switch (query.getCommand()) {
                    case "GET-FILE-LIST": {
                        Result result = new Result("SUCCESS");
                        File[] files = Server.FILES.listFiles();

                        if (files == null) {
                            writer.append(new Result("ERROR").addArgument("서버에서 파일 리스트를 가져올 수 없습니다").build());
                            writer.flush();
                            continue;
                        }

                        for (File file : files) {
                            if (file != null) {
                                BasicFileAttributes basicFileAttributes
                                        = Files.readAttributes(file.toPath(), BasicFileAttributes.class);

                                if (basicFileAttributes.isRegularFile()) {
                                    result.addArgument(file.getName());
                                    result.addArgument(String.valueOf(basicFileAttributes.size()));
                                }
                            }
                        }

                        writer.append(result.build()).flush();
                        break;
                    }
                    case "UPLOAD": {
                        String filename = query.getArguments().get(0);
                        String value = query.getArguments().get(1);

                        File targetFile = new File(Server.FILES, filename);

                        if (targetFile.exists()) {
                            writer.append(new Result("ERROR").addArgument("이름이 같은 파일이 존재합니다").build());
                            writer.flush();
                        }

                        downloadFile(filename, value);
                        break;
                    }
                    case "UPLOAD-FORCED": {
                        String filename = query.getArguments().get(0);
                        String value = query.getArguments().get(1);

                        downloadFile(filename, value);
                        break;
                    }
                    case "DOWNLOAD": {
                        String filename = query.getArguments().get(0);

                        File targetFile = new File(Server.FILES, filename);

                        if (!targetFile.exists()) {
                            writer.append(new Result("ERROR").addArgument("파일을 찾을 수 없었습니다").build());
                            writer.flush();
                        } else {
                            Result result = new Result("SUCCESS");

                            try (FileInputStream fis = new FileInputStream(targetFile)) {
                                byte[] bytes = fis.readAllBytes();
                                String value = Base64.getEncoder().encodeToString(bytes);

                                result.addArgument(value);
                            } catch (IOException e) {
                                writer.append(new Result("ERROR").addArgument("파일을 읽을 수 없었습니다").build());
                                writer.flush();
                                continue;
                            }

                            writer.append(result.build()).flush();
                        }
                        break;
                    }
                    case "CLOSE": {
                        Result result = new Result("BYE!");
                        writer.append(result.build()).flush();
                        break mainLoop;
                    }
                }
            }

        } catch (IOException |
                 NumberFormatException e) {
            System.err.println("에러 발생 " + e);
        } finally {
            try {
                reader.close();
            } catch (IOException ignore) {
            }
            try {
                writer.close();
            } catch (IOException ignore) {
            }
            try {
                client.close();
            } catch (IOException ignore) {
            }

            System.out.println(client.getInetAddress() + " 와 연결이 해제 됩니다");
            Server.clients.remove(client.getInetAddress());
        }

    }

    private void downloadFile(String filename, String value) throws IOException {
        byte[] bytes = Base64.getDecoder().decode(value);

        File output = new File(Server.FILES, filename);

        try (FileOutputStream fos = new FileOutputStream(output)) {
            fos.write(bytes);
            fos.flush();
        } catch (IOException e) {
            writer.append(new Result("ERROR").addArgument("파일을 다운로드하는데 실패하였습니다").build());
            writer.flush();
            return;
        }

        writer.append(new Result("SUCCESS").build()).flush();
    }
}
