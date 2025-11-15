package vn.poly.bagistore.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class MailService {

    @Autowired
    private JavaMailSender mailSender;

    /**
     * Send a welcome email with login information to the new employee.
     * Make sure SMTP properties are configured in application.properties:
     * spring.mail.host=smtp.gmail.com
     * spring.mail.port=587
     * spring.mail.username=your-gmail-address
     * spring.mail.password=your-app-password
     * spring.mail.properties.mail.smtp.auth=true
     * spring.mail.properties.mail.smtp.starttls.enable=true
     */
    public void sendWelcomeEmail(String to, String displayName, String username, String plainPassword){
        try{
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setTo(to);
            // prefer configured mail username as the sender address when available
            String fromAddress = "noreply@yourdomain.com";
            String fromName = "BagiStore Admin";
            try{
                if(mailSender instanceof JavaMailSenderImpl){
                    JavaMailSenderImpl impl = (JavaMailSenderImpl) mailSender;
                    String configuredUser = impl.getUsername();
                    if(configuredUser!=null && !configuredUser.trim().isEmpty()) fromAddress = configuredUser;
                }
            }catch(Exception ex){ /* ignore and fallback to default */ }
            helper.setFrom(fromAddress, fromName);
            helper.setSubject("Chào mừng bạn đến với BagiStore — Thông tin đăng nhập");

            String body = "<p>Xin chào " + escapeHtml(displayName) + ",</p>"
                    + "<p>Chào mừng bạn đã gia nhập đội ngũ BagiStore. Dưới đây là thông tin đăng nhập của bạn:</p>"
                    + "<ul>"
                    + "<li><strong>Tên đăng nhập:</strong> " + escapeHtml(username) + "</li>"
                    + "<li><strong>Mật khẩu :</strong> " + escapeHtml(plainPassword) + "</li>"
                    + "</ul>"
                    + "<p>Vui lòng đổi mật khẩu sau khi đăng nhập lần đầu để bảo mật tài khoản.</p>"
                    + "<p>Trân trọng,<br/>BagiStore Team</p>";

            helper.setText(body, true);
            // If mailSender isn't configured (no host), write the email to a local file for inspection.
            try{
                if(mailSender instanceof JavaMailSenderImpl){
                    JavaMailSenderImpl impl = (JavaMailSenderImpl) mailSender;
                    String host = impl.getHost();
                    if(host==null || host.trim().isEmpty()){
                        // write to file instead of sending
                        saveEmailToFile(to, helper.getMimeMessage());
                        System.err.println("MailService: mail host not configured — wrote email to file instead of sending to " + to);
                        return;
                    }
                }
                mailSender.send(msg);
            }catch(Exception ex){
                // if sending fails, fallback to saving to file for debugging
                saveEmailToFile(to, helper.getMimeMessage());
                System.err.println("MailService: failed to send email, saved to file. Reason: " + ex.getMessage());
            }
        }catch(Exception ex){
            // Log but don't rethrow to avoid breaking employee creation flow
            System.err.println("Failed sending welcome email to " + to + ": " + ex.getMessage());
        }
    }

    private void saveEmailToFile(String to, MimeMessage msg){
        try{
            java.nio.file.Path base = java.nio.file.Paths.get("uploads","emails");
            java.nio.file.Files.createDirectories(base);
            String fileName = "welcome_" + System.currentTimeMillis() + "_" + to.replaceAll("[^A-Za-z0-9@._-]","_") + ".html";
            java.nio.file.Path out = base.resolve(fileName);
            // try to extract content
            Object content = msg.getContent();
            String body = "";
            if(content instanceof String) body = (String)content;
            else {
                try(java.io.InputStream is = msg.getInputStream(); java.util.Scanner s = new java.util.Scanner(is, java.nio.charset.StandardCharsets.UTF_8.name())){
                    body = s.useDelimiter("\\A").hasNext() ? s.next() : "";
                }
            }
            java.nio.file.Files.writeString(out, body, java.nio.charset.StandardCharsets.UTF_8);
            System.err.println("MailService: email saved to " + out.toAbsolutePath());
        }catch(Exception e){
            System.err.println("MailService: failed to save email to file: " + e.getMessage());
        }
    }

    private String escapeHtml(String s){
        if(s==null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");
    }

}

