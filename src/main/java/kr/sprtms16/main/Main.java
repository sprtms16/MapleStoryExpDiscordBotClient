package kr.sprtms16.main;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.FileUpload;
import net.sourceforge.tess4j.Tesseract;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Main {
    public static Robot robot = null;

    static Tesseract instance = new Tesseract();
    static JDA jda = JDABuilder.createDefault("여기에 토큰")
            .setStatus(OnlineStatus.ONLINE)
            .setAutoReconnect(true)
            .enableIntents(GatewayIntent.MESSAGE_CONTENT)
            .build();
    //https://discord.com/api/oauth2/authorize?client_id=1033274341107970130&permissions=8&scope=bot
    static List<String> lastExpMessages = new ArrayList<>();
    static TextChannel alertChannel = null;

    public static void main(String[] args) {
        double mx = 1000;
        double my = 1000;
        jda.addEventListener(new ListenerAdapter(){
            @Override
            public void onMessageReceived(@NotNull MessageReceivedEvent event) {
                User user = event.getAuthor();
                TextChannel channel = event.getChannel().asTextChannel();
                Message message = event.getMessage();
                if(user.isBot()) return;
                if(message.getContentRaw().charAt(0) == '!'){
                    String[] args = message.getContentRaw().substring(1).split(" ");
                    if(args.length <= 0) return;
                    if(args[0].equalsIgnoreCase("여기")){
                        if(alertChannel != null)
                            alertChannel.sendMessage("이곳의 알람은 해제됩니다.").queue();
                        alertChannel = channel;
                        channel.sendMessage("다음부터 이곳으로 경뿌 알람이 울립니다.").queue();
                    }
                }
            }
        });
//        URL url = Main.class.getClassLoader().getResource("tessdata");

//        Path path = new File("src"+File.separator+"main"+File.separator+"resources"+File.separator+"tessdata").toPath();

        try {
            robot = new Robot();
//            instance.setDatapath("src"+File.separator+"main"+File.separator+"resources"+File.separator+"tessdata");
            instance.setLanguage("kor+eng");
            // 마우스가 화면 좌상단 100, 100 이내이면 프로그램 종료.
            while (mx > 100 || my > 100) {
                Point point = MouseInfo.getPointerInfo().getLocation();
                mx = point.getLocation().getX();
                my = point.getLocation().getY();
                HashMap<String, HWND> hwndMap = checkHwnd();
                HWND hWnd = castHwnd(hwndMap.get("MapleStoryClass"));
                if (hWnd != null) {
                    String curWinName = getClassNameFromHandle(hWnd);
                    if (curWinName.equals("MapleStoryClass")) {
                        // 종료
                        File outputfile = new File("megaphone.jpg");
                        BufferedImage image = crop(5, 668+70-22, 1366/2-110-10-7, 768-668-70-15+23, capture(hWnd));
                        ImageIO.write(image, "jpg", outputfile);
                        String result = instance.doOCR(image);
                        if(lastExpMessages.stream().noneMatch(result::contains)) {
                            boolean isExpAlert = false;
                            String detectMessage = "";
                            if(result.contains("경뿌")) {
                                detectMessage = "경뿌";
                                isExpAlert = true;
                            } else if(result.contains("마빌")){
                                detectMessage = "마빌";
                                isExpAlert = true;
                            } else if(result.contains("ㄱㅃ")){
                                detectMessage = "ㄱㅃ";
                                isExpAlert = true;
                            } else if(result.contains("ㅁㅂ")) {
                                detectMessage = "ㅁㅂ";
                                isExpAlert = true;
                            } else if(result.contains("ㅈㅋ")){
                                detectMessage = "ㅈㅋ";
                                isExpAlert = true;
                            } else if(result.contains("내썹")){
                                detectMessage = "내썹";
                                isExpAlert = true;
                            } else if(result.contains("ㄴㅆ")){
                                detectMessage = "ㄴㅆ";
                                isExpAlert = true;
                            } else if(result.contains("경험치")){
                                detectMessage = "경험치";
                                isExpAlert = true;
                            } else if(result.contains("연탐")){
                                detectMessage = "연탐";
                                isExpAlert = true;
                            } else if(result.contains("쿰")){
                                detectMessage = "쿰";
                                isExpAlert = true;
                            }
                            if(isExpAlert && alertChannel!= null){
                                lastExpMessages = Arrays.stream(result.split("\\n")).toList();
                                String finalDetectMessage = detectMessage;
                                alertChannel.sendMessage("`"+finalDetectMessage +"`이(가) 발견되었습니다.").addFiles(FileUpload.fromData(outputfile, "image.jpg")).queue();
                                Thread.sleep(10000);
                            }
                        }
                    }
                    Thread.sleep(1000);
                } else {
                    Thread.sleep(100);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.err.println("사용자 명령으로 종료.");
        System.exit(0);
    }

    public static BufferedImage capture(HWND hWnd) {

        WinDef.HDC hdcWindow = User32.INSTANCE.GetDC(hWnd);
        WinDef.HDC hdcMemDC = GDI32.INSTANCE.CreateCompatibleDC(hdcWindow);

        RECT bounds = new RECT();
        User32Extra.INSTANCE.GetClientRect(hWnd, bounds);

        int width = bounds.right - bounds.left;
        int height = bounds.bottom - bounds.top;

        WinDef.HBITMAP hBitmap = GDI32.INSTANCE.CreateCompatibleBitmap(hdcWindow, width, height);

        WinNT.HANDLE hOld = GDI32.INSTANCE.SelectObject(hdcMemDC, hBitmap);
        GDI32Extra.INSTANCE.BitBlt(hdcMemDC, 0, 0, width, height, hdcWindow, 0, 0, WinGDIExtra.SRCCOPY);

        GDI32.INSTANCE.SelectObject(hdcMemDC, hOld);
        GDI32.INSTANCE.DeleteDC(hdcMemDC);

        WinGDI.BITMAPINFO bmi = new WinGDI.BITMAPINFO();
        bmi.bmiHeader.biWidth = width;
        bmi.bmiHeader.biHeight = -height;
        bmi.bmiHeader.biPlanes = 1;
        bmi.bmiHeader.biBitCount = 32;
        bmi.bmiHeader.biCompression = WinGDI.BI_RGB;

        Memory buffer = new Memory((long) width * height * 4);
        GDI32.INSTANCE.GetDIBits(hdcWindow, hBitmap, 0, height, buffer, bmi, WinGDI.DIB_RGB_COLORS);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, width, height, buffer.getIntArray(0, width * height), 0, width);

        GDI32.INSTANCE.DeleteObject(hBitmap);
        User32.INSTANCE.ReleaseDC(hWnd, hdcWindow);

        return image;

    }

    public static BufferedImage crop(int x, int y, int width, int height, BufferedImage buffer) {
        BufferedImage dest = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = dest.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.drawImage(buffer, 0, 0, width, height, x, y, x + width, y + height,
                null);
        g.dispose();
        return dest;
    }

    public static HWND castHwnd(Object obj) {

        try {
            return (HWND) obj;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static HashMap<String, HWND> checkHwnd() {

        final HashMap<String, HWND> hwndMap = new HashMap<>();

        //            int count = 0;
        User32.INSTANCE.EnumWindows((hWnd, arg1) -> {
            char[] windowText = new char[512];
            User32.INSTANCE.GetWindowText(hWnd, windowText, 512);
            String wText = Native.toString(windowText);
            RECT rectangle = new RECT();
            User32.INSTANCE.GetWindowRect(hWnd, rectangle);
            // get rid of this if block if you want all windows
            // regardless
            // of whether
            // or not they have text
            // second condition is for visible and non minimised windows
            if (wText.isEmpty() || !(User32.INSTANCE.IsWindowVisible(hWnd) && rectangle.left > -32000)) {
                return true;
            }

            String clsName = getClassNameFromHandle(hWnd);
            if (clsName.length() > 0) {

                 StringBuffer buff = new StringBuffer();
                //
                // buff.append("번호:" + (++count));
                // buff.append(",텍스트:" + wText);
//                 buff.append("," + "위치:(");
//                 buff.append(rectangle.left).append(",").append(rectangle.top).append(")~(");
//                 buff.append(rectangle.right).append(",").append(rectangle.bottom);
//                 buff.append(")," + "클래스네임:").append(clsName);
//                //
//                 System.out.println(buff);

                hwndMap.put(clsName, hWnd);
            }

            return true;
        }, null);

        return hwndMap;
    }

    public static String getClassNameFromHandle(HWND hWnd) {
        if (hWnd == null) {
            return "";
        }

        // 핸들의 클래스 네임 얻기
        char[] c = new char[512];
        User32.INSTANCE.GetClassName(hWnd, c, 512);
        return String.valueOf(c).trim();
    }
}
