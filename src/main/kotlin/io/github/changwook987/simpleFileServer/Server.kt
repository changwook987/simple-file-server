package io.github.changwook987.simpleFileServer

import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.*

val FILES = File("Files")
val threads = HashMap<InetAddress, ClientThread>()
val accounts = mapOf("admin" to "1234")

const val END = "/END/"

fun main(args: Array<String>) {
    val port = if (args.isEmpty()) {
        print("port > ")
        readln()
    } else {
        args[0]
    }.toInt()

    FILES.mkdir()

    ServerSocket(port).use { server ->
        println("~WELCOME TO MY FILE SERVER~")
        while (true) {
            val client = server.accept()
            val address = client.inetAddress

            println("$address is connected")

            threads[address]?.let(Thread::interrupt)

            val thread = ClientThread(client)

            thread.isDaemon = true
            thread.start()

            threads[address] = thread
        }
    }
}


class ClientThread(private val socket: Socket) : Thread() {
    private var isLogin = false

    override fun run() {
        val iter = sequence {
            socket.getInputStream().bufferedReader().use { reader ->
                while (true) yield(reader.readLine() ?: break)
            }
        }.iterator()

        fun end() {
            while (iter.next() != END);
        }

        val writer = socket.getOutputStream().bufferedWriter()

        try {
            while (iter.hasNext()) {
                when (iter.next()) {
                    "LOGIN" -> {
                        val id = iter.next()
                        val pw = iter.next()

                        writer.appendLine(
                            if (id in accounts && accounts[id] == pw) {
                                "SUCCESS"
                            } else {
                                "ERROR"
                            }
                        )

                        end()

                        writer.appendLine(END)
                        writer.flush()

                        isLogin = true
                    }
                    "FILE-LIST" -> {
                        end()

                        if (isLogin) {
                            val list = FILES.listFiles()?.filterNotNull() ?: emptyList()

                            for (file in list) {
                                writer.appendLine(file.name)
                                writer.appendLine("${file.length()}")
                            }
                        } else {
                            writer.appendLine("ERROR")
                            writer.appendLine("NOT LOGGED IN")
                        }

                        writer.appendLine(END)
                        writer.flush()
                    }
                    "FILE-CHECK" -> {
                        if (isLogin) {
                            val filename = iter.next()
                            writer.appendLine(
                                if (File(FILES, filename).exists()) {
                                    "ERROR"
                                } else {
                                    "SUCCESS"
                                }
                            )
                        } else {
                            writer.appendLine("ERROR")
                            writer.appendLine("NOT LOGGED IN")
                        }

                        end()

                        writer.appendLine(END)
                        writer.flush()
                    }
                    "UPLOAD" -> {
                        if (isLogin) {
                            val filename = iter.next()

                            File(FILES, filename).outputStream().use { fos ->

                                while (true) {
                                    val line = iter.next()

                                    if (line == END) break
                                    else fos.write(Base64.getDecoder().decode(line))
                                }

                                fos.flush()

                                writer.appendLine("SUCCESS")
                                writer.appendLine(END)
                                writer.flush()
                            }
                        } else {
                            writer.appendLine("ERROR")
                            writer.appendLine("NOT LOGGED IN")
                            writer.appendLine(END)
                            end()
                            writer.flush()
                        }
                    }
                    "DOWNLOAD" -> {
                        val filename = iter.next()
                        end()

                        if (isLogin) {

                            val file = File(FILES, filename)

                            if (!file.exists()) {
                                writer.appendLine("ERROR")
                                writer.appendLine(END)
                            } else {
                                val encoder = Base64.getEncoder()

                                writer.appendLine("SUCCESS")
                                writer.appendLine("${file.length()}")

                                file.inputStream().use { fis ->
                                    while (fis.available() > 0) {
                                        val size = minOf(fis.available(), 1024)
                                        writer.appendLine(encoder.encodeToString(fis.readNBytes(size)))
                                    }
                                }

                                writer.appendLine(END)
                            }
                        } else {
                            writer.appendLine("ERROR")
                            writer.appendLine("NOT LOGGED IN")
                            writer.appendLine(END)
                        }

                        writer.flush()
                    }
                    "LOGOUT" -> {
                        end()

                        writer.appendLine("SUCCESS")
                        writer.appendLine(END)
                        writer.flush()

                        writer.close()

                        break
                    }
                    else -> {
                        end()
                        writer.appendLine("ERROR")
                        writer.appendLine("UNKNOWN COMMAND")
                        writer.appendLine(END)
                        writer.flush()
                    }
                }
            }
        } catch (e: Exception) {
            try {
                writer.close()
            } catch (_: IOException) {
            }
        }

        threads.remove(socket.inetAddress)
        println("${socket.inetAddress} is disconnected")
    }
}