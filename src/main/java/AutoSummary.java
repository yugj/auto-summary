import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * java AutoSummary
 *
 * @author yugj
 * @date 19/4/15 20:28.
 */
public class AutoSummary {

    private static List<String> ignoredFiles = new ArrayList<String>();
    private static String basePath;
    private static String usage;
    private static final String SUMMARY_MD = "SUMMARY.md";
    private static final String README_MD = "README.md";


    /**
     * SUMMARY.md String
     */
    private static StringBuilder summary = new StringBuilder("# Summary").append("\n");

    static {
        ignoredFiles.add("_book");
        ignoredFiles.add(".git");
        ignoredFiles.add(".DS_Store");
        ignoredFiles.add("node_modules");
        ignoredFiles.add(SUMMARY_MD);

        usage = "usage:  java AutoSummary {book-path}";
    }

    /**
     * 遍历文件
     *
     * @param path 文件路径
     * @throws IOException ex
     */
    private void traversalFile(String path) throws IOException {
        File baseFile = new File(path);
        File[] files = baseFile.listFiles();
        if (files == null) {
            System.out.println("file read error,file path:" + path);
            System.exit(1);
        }


        //sort
        List fileList = Arrays.asList(files);
        Collections.sort(fileList, new Comparator<File>() {
            public int compare(File o1, File o2) {
                if (o1.getName().equals(README_MD)) {
                    return -1;
                }
                return o1.getName().compareTo(o2.getName());
            }
        });

        for (File file : files) {
            if (ignoredFiles.contains(file.getName())) {
                continue;
            }

            if (file.isFile() && !file.getName().endsWith(".md")) {
                continue;
            }

            //当前层级 生成行
            String alias = generateFileAlias(file);
            String fileSummaryLine = generateSummaryLine(alias, file.getPath(), alias, 0);
            summary.append(fileSummaryLine);

            if (file.isFile()) {
                traversalMd(file);
            } else {
                traversalFile(file.getPath());
            }
        }
    }

    /**
     * 取第一个 H1 标题 取不到则取文件名
     *
     * @param file file
     * @return Heading
     * @throws IOException io ex
     */
    private String generateFileAlias(File file) throws IOException {

        //目录文件优先使用下划线命名分隔命名，无则使用文件夹名字
        if (file.isDirectory()) {
            String[] fileSplit = file.getName().split("_");
            if (fileSplit.length == 2) {
                return fileSplit[1];
            } else {
                return file.getName();
            }
        }

        FileReader fr = new FileReader(file);
        BufferedReader bf = new BufferedReader(fr);
        String content;
        while ((content = bf.readLine()) != null) {
            if (isFirstHeading(content)) {
                String[] firstHeading = content.split(" ");
                return firstHeading[1];
            }
        }

        fr.close();
        bf.close();
        return file.getName();
    }

    private String generateSummaryLine(String alias, String mdPath, String anchor, int headingLevel) {

        String relatePath = mdPath.replace(basePath, "");
        int relateLength = appearNumber(relatePath, "/");

        int appendTimes = relateLength + headingLevel;

        String space = getSummaryLineSpace(appendTimes);

        StringBuilder sb = new StringBuilder(space);


        File mFile = new File(mdPath);
        //文件夹路径取文件里面第一个文件作为锚点
        String firstFileInDir = null;
        if (mFile.isDirectory()) {
            File firstFile = mFile.listFiles()[0];
            firstFileInDir = firstFile.getPath().replace(basePath, "");
        }

        relatePath = firstFileInDir != null ? firstFileInDir : relatePath;

        sb.append("* [").append(alias).append("]");
        sb.append("(").append(relatePath).append("#").append(anchor).append(")").append("\n");
        return sb.toString();
    }

    private  int appearNumber(String srcText, String findText) {
        int count = 0;
        Pattern p = Pattern.compile(findText);
        Matcher m = p.matcher(srcText);
        while (m.find()) {
            count++;
        }
        return count;
    }

    private String getSummaryLineSpace(Integer times) {

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append("  ");
        }
        return sb.toString();
    }

    /**
     * md only
     *
     * @param file input file
     */
    private void traversalMd(File file) throws IOException {

        if (!file.getName().endsWith(".md")) {
            return;
        }

        FileReader fr = new FileReader(file);
        BufferedReader bf = new BufferedReader(fr);
        String content;

        boolean firstMatch = false;
        while ((content = bf.readLine()) != null) {
            if (isFirstHeading(content)) {

                if (!firstMatch) {
                    firstMatch = true;
                    continue;
                }
                String[] firstHeading = content.split(" ");
                String summaryLine = generateSummaryLine(firstHeading[1], file.getPath(), firstHeading[1].toLowerCase(), 1);
                summary.append(summaryLine);
            } else if (isSecondHeading(content)) {
                String[] secondHeading = content.split(" ");
                String summaryLine = generateSummaryLine(secondHeading[1], file.getPath(), secondHeading[1].toLowerCase(), 2);
                summary.append(summaryLine);
            }
        }

        bf.close();
        fr.close();
    }

    /**
     * 临时用下
     *
     * @param content 正文
     * @return 是否h1
     */
    private boolean isFirstHeading(String content) {
        content = content.trim();
        return content.startsWith("# ");

    }

    private boolean isSecondHeading(String content) {
        content = content.trim();
        return content.startsWith("## ");
    }

    public static void main(String[] args) throws IOException {

        if (args.length == 0 || "".equals(args[0].trim())) {
            System.out.println(usage);
            System.exit(1);
        }

        AutoSummary autoSummary = new AutoSummary();
        basePath = args[0];
        if (!basePath.endsWith(File.separator)) {
            basePath = basePath + File.separator;
        }

        System.out.println("base path :" + basePath);

        autoSummary.traversalFile(args[0]);

        System.out.println(summary.toString());

        String summaryPath = basePath + File.separator + SUMMARY_MD;
        boolean success = new File(summaryPath).delete();
        if (!success) {
            System.out.println("delete summary failed");
            System.exit(1);
        }

        FileWriter writer;
        writer = new FileWriter(basePath + File.separator + SUMMARY_MD);
        writer.write(summary.toString());

        writer.flush();
        writer.close();

        System.exit(0);
    }

}
