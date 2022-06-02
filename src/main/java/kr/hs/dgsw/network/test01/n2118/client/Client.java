package kr.hs.dgsw.network.test01.n2118.client;

import kr.hs.dgsw.network.test01.n2118.Query;
import kr.hs.dgsw.network.test01.n2118.Result;

import java.io.*;
import java.net.Socket;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class Client {
    public static void main(String[] args) {
        if (args.length < 2) {
            errorAndExit("host와 port정보가 누락되었습니다");
            return;
        }

        String host;
        int port;

        File downloadFolder = new File("./downloads");
        downloadFolder.mkdirs();

        try {
            host = args[0];
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            errorAndExit("포트번호가 잘못되었습니다");
            return;
        }

        try (
                BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

                Socket socket = new Socket(host, port);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        ) {
            System.out.println("**서버에 성공적으로 접속하였습니다**");
            
            while (true) {
                System.out.println("로그인");

                System.out.print("ID >");
                String id = console.readLine();

                System.out.print("PW >");
                String pw = console.readLine();

                String query = new Query("LOGIN")
                        .addArgument(id)
                        .addArgument(pw)
                        .build();

                writer.append(query).flush();

                Result result = Result.receive(reader);

                if (result.isSuccess()) break;
                System.out.println("**아이디 또는 비밀번호가 틀렸습니다**");
            }

            System.out.println("**로그인 완료**");

            while (true) {
                String line = console.readLine();

                if (line.startsWith("/파일목록")) {
                    String query = new Query("GET-FILE-LIST").build();

                    writer.append(query).flush();

                    Result result = Result.receive(reader);

                    if (result.isSuccess()) {
                        System.out.println("**파일 목록**");

                        List<String> arguments = result.getArguments();

                        Iterator<String> iter = arguments.iterator();

                        while (iter.hasNext()) {
                            String filename = iter.next();
                            long size = Long.parseLong(iter.next());

                            System.out.printf("%-50s %25dB\n", filename, size);
                        }

                        System.out.printf("**%d개 파일**\n", arguments.size() / 2);
                    }
                } else if (line.startsWith("/업로드")) {

                    String[] arguments;

                    try {
                        arguments = line.substring(5).split(" ");
                    } catch (IndexOutOfBoundsException e) {
                        System.err.println("/업로드 <파일 경로> [파일 이름]");
                        continue;
                    }

                    File file;
                    byte[] bytes;
                    String filename;
                    String filepath;

                    if (arguments.length == 0) {
                        System.err.println("/업로드 <파일 경로> [파일 이름]");
                        continue;
                    } else if (arguments.length == 1) {
                        filepath = arguments[0];

                        file = new File(filepath);
                        filename = file.getName();
                    } else {
                        filepath = arguments[0];
                        filename = arguments[1];

                        file = new File(filepath);
                    }

                    if (!file.isFile()) {
                        System.err.println("파일이 없습니다");
                        continue;
                    }

                    try (FileInputStream inputStream = new FileInputStream(file)) {
                        bytes = inputStream.readAllBytes();
                    } catch (IOException e) {
                        System.err.println("파일을 읽을 수 없습니다");
                        continue;
                    }

                    String value = Base64.getEncoder().encodeToString(bytes);

                    String query = new Query("UPLOAD")
                            .addArgument(filename)
                            .addArgument(value)
                            .build();

                    writer.append(query).flush();

                    Result result = Result.receive(reader);

                    if (result.isSuccess()) {
                        System.out.println("업로드 되었습니다");
                    } else {
                        List<String> arg = result.getArguments();
                        String cause = null;

                        if (!arg.isEmpty()) cause = arg.get(0);

                        if (cause != null && cause.equals("이름이 같은 파일이 존재합니다")) {

                            System.out.println("같은 이름의 파일이 존재합니다.");

                            while (true) {
                                System.out.print("덮어쓰시겠습니까 (Y, N) ");
                                if ((line = console.readLine()) != null) {
                                    line = line.toLowerCase(Locale.ROOT);

                                    if (line.equals("y")) {
                                        query = new Query("UPLOAD-FORCED")
                                                .addArgument(filename)
                                                .addArgument(value)
                                                .build();

                                        writer.append(query).flush();

                                        result = Result.receive(reader);

                                        if (result.isSuccess()) {
                                            System.out.println("성공적으로 업로드 되었습니다");
                                        } else {
                                            System.out.println("업로드에 실패하였습니다");
                                        }

                                        break;
                                    } else if (line.equals("n")) {
                                        System.out.println("업로드를 취소합니다");
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } else if (line.startsWith("/다운로드")) {
                    String filename = line.substring(5).trim();

                    String query = new Query("DOWNLOAD")
                            .addArgument(filename)
                            .build();

                    writer.append(query).flush();

                    Result result = Result.receive(reader);

                    if (result.isSuccess()) {
                        String value = result.getArguments().get(0);
                        byte[] bytes = Base64.getDecoder().decode(value);

                        File output = new File(downloadFolder, filename);

                        try (FileOutputStream fos = new FileOutputStream(output)) {
                            fos.write(bytes);
                            fos.flush();

                            System.out.println("다운로드에 성공했습니다");
                        } catch (IOException e) {
                            System.out.println("파일 저장에 실패하였습니다");
                        }
                    }
                } else if (line.startsWith("/접속종료")) {
                    String query = new Query("CLOSE").build();
                    writer.append(query).flush();

                    Result.receive(reader);
                    break;
                }
            }
        } catch (Exception e) {
            errorAndExit("예외가 발생하여 종료합니다");
        }
    }

    private static void errorAndExit(String errorMessage) {
        System.err.println(errorMessage);
        System.exit(-1);
    }
}
