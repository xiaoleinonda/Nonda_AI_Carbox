package us.nonda.commonibrary.utils;

import android.graphics.Bitmap;
import android.os.Environment;
import us.nonda.commonibrary.MyLog;

import java.io.*;
import java.util.LinkedList;

/**
 * author : shangrong
 * date : 2019/5/23 2:05 PM
 * description :文件工具类
 */
public class FileUtils {
    /**
     * 读取txt文件的内容
     *
     * @param filePath 想要读取的文件对象
     * @return 返回文件内容
     */
    public static String txt2String(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));//构造一个BufferedReader类来读取文件
            String s = null;
            while ((s = br.readLine()) != null) {//使用readLine方法，一次读一行
                result.append(System.lineSeparator() + s);
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result.toString();
    }


    /**
     * 写入TXT文件
     */
    public static boolean writeTxtFile(String content, String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return false;
        }

        RandomAccessFile mm = null;
        boolean flag = false;
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(content.getBytes("utf-8"));
            fileOutputStream.close();
            flag = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }

    /**
     * Checks if is sd card available.检查SD卡是否可用
     */
    public static boolean isSdCardAvailable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    /**
     * Gets the SD root file.获取SD卡根目录
     */
    public static File getSDRootFile() {
        if (isSdCardAvailable()) {
            return Environment.getExternalStorageDirectory();
        } else {
            return null;
        }
    }

    /**
     * 获取导入图片文件的目录信息
     */
    public static File getBatchImportDirectory() {
        File sdRootFile = getSDRootFile();
        File file = null;
        if (sdRootFile != null && sdRootFile.exists()) {
            file = new File(sdRootFile, "Face-Import");
            if (!file.exists()) {
                file.mkdirs();
            }
        }
        return file;
    }

    /**
     * 获取导入图片成功的目录信息
     */
    public static File getBatchImportSuccessDirectory() {
        File sdRootFile = getSDRootFile();
        File file = null;
        if (sdRootFile != null && sdRootFile.exists()) {
            file = new File(sdRootFile, "Success-Import");
            if (!file.exists()) {
                file.mkdirs();
            }
        }
        return file;
    }

    /**
     * 判断文件是否存在
     */
    public static File isFileExist(String fileDirectory, String fileName) {
        File file = new File(fileDirectory + "/" + fileName);
        try {
            if (!file.exists()) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
        return file;
    }

    /**
     * 删除文件
     */
    public static void deleteFile(String filePath) {
        try {
            // 找到文件所在的路径并删除该文件
            File file = new File(filePath);
            file.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * 获取不带扩展名的文件名
     * */
    public static String getFileNameNoEx(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if ((dot > -1) && (dot < (filename.length()))) {
                return filename.substring(0, dot);
            }
        }
        return filename;
    }

    /**
     * 保存图片
     */
    public static boolean saveBitmap(File file, Bitmap bitmap) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static boolean copyFile(String oldPath, String newPath) {
        InputStream inStream = null;
        FileOutputStream fs = null;
        boolean result = false;
        try {
            int bytesum = 0;
            int byteread = 0;
            File oldfile = new File(oldPath);
            // 判断目录是否存在
            File newfile = new File(newPath);
            File newFileDir = new File(newfile.getPath().replace(newfile.getName(), ""));
            if (!newFileDir.exists()) {
                newFileDir.mkdirs();
            }
            if (oldfile.exists()) { // 文件存在时
                inStream = new FileInputStream(oldPath); // 读入原文件
                fs = new FileOutputStream(newPath);
                byte[] buffer = new byte[1444];
                int length;
                while ((byteread = inStream.read(buffer)) != -1) {
                    bytesum += byteread; // 字节数 文件大小
                    System.out.println(bytesum);
                    fs.write(buffer, 0, byteread);
                }
                result = true;
            } else {
                result = false;
            }
        } catch (Exception e) {
            System.out.println("复制单个文件操作出错");
            e.printStackTrace();
        } finally {
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fs != null) {
                try {
                    fs.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    public static String saveBitmapToSDCard(int[] argb, int width, int height, String sdcardPath, String imgName) {
        if (argb == null || argb.length <= 0) {
            return "";
        }
        try {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {      // 获取SDCard指定目录下

                File dirFile = new File(sdcardPath);  //目录转化成文件夹
                if (!dirFile.exists()) {                //如果不存在，那就建立这个文件夹
                    dirFile.mkdirs();
                }                            //文件夹有啦，就可以保存图片啦
                File file = new File(sdcardPath, imgName + ".jpeg");// 在SDcard的目录下创建图片文,以当前时间为其命名

                Bitmap bitmap = Bitmap.createBitmap(width,
                        height, Bitmap.Config.ARGB_8888);
                bitmap.setPixels(argb, 0, width, 0, 0, width, height);

                FileOutputStream out = null;
                try {
                    out = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    return file.getAbsolutePath();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";

    }

    /**
     * 文件转为二进制数组
     *
     * @param file
     * @return
     */

    public static byte[] fileToByte(File file) throws IOException {
        byte[] bytes = null;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            bytes = new byte[(int) file.length()];
            fis.read(bytes);
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            fis.close();
        }
        return bytes;
    }

    /**
     * 遍历获取文件夹下所有MP4文件
     *
     * @param path
     */
    public static File[] traverseFolderGetMP4(String path) {
        int fileNum = 0, folderNum = 0;

        File file = new File(path);
        if (file.exists()) {
            LinkedList<File> fileList = new LinkedList<File>();
            LinkedList<File> list = new LinkedList<File>();

            File[] files = file.listFiles();
            for (File file2 : files) {
                if (file2.isDirectory()) {
                    list.add(file2);
                    folderNum++;
                } else {
                    if (file2.getName().endsWith("mp4")) {
                        fileList.add(file2);
                        fileNum++;
                    }
                }
            }
            File temp_file;
            while (!list.isEmpty()) {
                temp_file = list.removeFirst();
                files = temp_file.listFiles();
                for (File file2 : files) {
                    if (file2.isDirectory()) {
                        list.add(file2);
                        folderNum++;
                    } else {
                        if (file2.getName().endsWith("mp4")) {
                            fileList.add(file2);
                            fileNum++;
                        }
                    }
                }
            }
            MyLog.d("上传文件", "文件夹共有:" + folderNum + ",文件共有:" + fileNum);
            return fileList.toArray(new File[0]);
        } else {
            MyLog.d("上传文件", "文件不存在!");
            return null;
        }
    }

    /**
     * 迭代删除文件夹
     *
     * @param dirPath 文件夹路径
     */
    public static void deleteDir(String dirPath) {
        File file = new File(dirPath);
        if (file.isFile()) {
            file.delete();
        } else {
            File[] files = file.listFiles();
            if (files == null) {
                file.delete();
            } else {
                for (File file1 : files) {
                    deleteDir(file1.getAbsolutePath());
                }
                file.delete();
            }
        }
    }
}
