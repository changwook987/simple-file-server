package io.github.changwook987.simpleFileServer

import java.io.File
import java.net.Socket
import java.util.*

fun main(args: Array<String>) {
    val argsIter = args.iterator()
    val downloadFolder = File("downloads")

    downloadFolder.mkdir()

    val hostname = if (argsIter.hasNext()) argsIter.next()
    else {
        print("hostname > ")
        readln()
    }
    val port = if (argsIter.hasNext()) argsIter.next().toInt()
    else {
        print("port > ")
        readln().toIntOrNull() ?: return
    }

    Socket(hostname, port).use { socket ->
        val serverIter = sequence {
            socket.getInputStream().bufferedReader().use { reader ->
                while (true) yield(reader.readLine() ?: break)
            }
        }.iterator()

        fun end() {
            do {
                val line = serverIter.next()
            } while (line != END)
        }

        val writer = socket.getOutputStream().bufferedWriter()

        try {
            while (true) {
                print("아이디:")
                val id = readln()
                print("비밀번호:")
                val pw = readln()

                writer.appendLine("LOGIN")
                writer.appendLine(id)
                writer.appendLine(pw)
                writer.appendLine(END)

                writer.flush()

                when (serverIter.next()) {
                    "SUCCESS" -> {
                        end()
                        break
                    }

                    else -> {
                        end()
                        continue
                    }
                }
            }

            while (true) {
                val lineIter = readln().split(" ").iterator()

                when (lineIter.next()) {
                    "/파일목록" -> {
                        writer.appendLine("FILE-LIST")
                        writer.appendLine(END)
                        writer.flush()

                        println("파일목록을 조회합니다")
                        var i = 0

                        while (true) {
                            val filename = serverIter.next()
                            if (filename == END) break
                            val filesize = serverIter.next()
                            if (filesize == END) break

                            i++
                            println("%-30s%15sB".format(filename, filesize))
                        }

                        println("총 ${i}개의 결과가 조회되었습니다")
                    }

                    "/업로드" -> {
                        val path = lineIter.next()
                        val file = File(path)

                        if (!file.exists()) {
                            println("존재하지않는 파일")
                            continue
                        } else if (!file.isFile) {
                            println("파일이 아닙니다")
                            continue
                        }

                        val filename = if (lineIter.hasNext()) lineIter.next() else file.name

                        writer.appendLine("FILE-CHECK")
                        writer.appendLine(filename)
                        writer.appendLine(END)
                        writer.flush()

                        val res = serverIter.next()
                        end()

                        if (res != "SUCCESS") {
                            print("동일한 파일이 있습니다. 덮어쓰시겠습니까? (y/n)")
                            if (readln().lowercase() != "y") {
                                continue
                            }
                        }

                        writer.appendLine("UPLOAD")
                        writer.appendLine(filename)

                        val encoder = Base64.getEncoder()

                        var i = 0.0
                        val len = file.length()

                        file.inputStream().use { fis ->
                            while (fis.available() > 0) {
                                val size = minOf(fis.available(), 1024)
                                i += size
                                writer.appendLine(encoder.encodeToString(fis.readNBytes(size)))

                                print("%.0f%%\r".format(i / len * 100))
                            }
                        }

                        writer.appendLine(END)
                        writer.flush()

                        if (serverIter.next() == "SUCCESS") {
                            println("완료!")
                        } else {
                            println("전송과정에서 오류가 발생했습니다")
                        }

                        end()
                    }

                    "/다운로드" -> {

                        if (!lineIter.hasNext()) {
                            println("/다운로드 <파일이름>")
                            continue
                        }

                        val filename = lineIter.next()

                        writer.appendLine("DOWNLOAD")
                        writer.appendLine(filename)
                        writer.appendLine(END)
                        writer.flush()

                        if (serverIter.next() == "SUCCESS") {
                            val len = serverIter.next().toLong()
                            File(downloadFolder, filename).outputStream().use { fos ->
                                var i = 0.0

                                while (true) {
                                    val line = serverIter.next()

                                    if (line == END) break
                                    else {
                                        val bytes = Base64.getDecoder().decode(line)

                                        fos.write(bytes)
                                        i += bytes.size
                                    }

                                    print("%.0f%%\r".format(i / len * 100))
                                }

                                fos.flush()
                                println("다운받았습니다")
                            }
                        } else {
                            end()
                            println("해당 파일이 존재하지 않습니다")
                        }
                    }

                    "/접속종료" -> {
                        writer.appendLine("LOGOUT")
                        writer.appendLine(END)
                        writer.flush()

                        if (serverIter.next() == "SUCCESS") break
                    }
                }
            }

        } catch (_: NoSuchElementException) {
        }

        writer.close()
        println("bye")
    }
}