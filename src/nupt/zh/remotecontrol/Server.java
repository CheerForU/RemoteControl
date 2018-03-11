package nupt.zh.remotecontrol;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * 服务端
 * 
 * @author zh
 *
 */
public class Server extends JPanel implements Runnable {

	private static final long serialVersionUID = -927388268343256207L;
	private ServerSocket server;
	private Thread thread;
	private Robot controlMouseRobot;
	private JButton releaseConnect;
	private JLabel label;

	public Server(String ip, int port) throws IOException, AWTException {
		server = new ServerSocket(port, 1, InetAddress.getByName(ip));
		thread = new Thread(this);
		controlMouseRobot = new Robot();

		label = new JLabel("监听" + ip + ":" + port);

		releaseConnect = new JButton("断开连接");
		this.add(releaseConnect);
		releaseConnect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				stop();
			}
		});
		this.add(label);
		this.add(releaseConnect);
	}

	public void start() {
		thread.start();
	}

	@SuppressWarnings("deprecation")
	public void stop() {
		label.setText("已断开连接");
		thread.stop();
		try {
			server.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {

		while (true) {
			ObjectInputStream request = null;
			ObjectOutputStream response = null;
			try {
				Socket client = server.accept();
				label.setText("被控制中");
				request = new ObjectInputStream(new BufferedInputStream(client.getInputStream()));
				ControlCarrier carrier = (ControlCarrier) request.readObject();

				System.out.println("收到命令:" + carrier);

				if (carrier.getMouseX() != -1 && carrier.getMouseY() != -1) {
					controlMouseRobot.mouseMove(carrier.getMouseX(), carrier.getMouseY());
				}

				if (carrier.getMousePressBtn() != -1) {
					controlMouseRobot.mousePress(carrier.getMousePressBtn());
				}

				if (carrier.getMouseReleaseBtn() != -1) {
					controlMouseRobot.mouseRelease(carrier.getMouseReleaseBtn());
				}

				if (carrier.getWheelAmt() != -1) {
					controlMouseRobot.mouseWheel(carrier.getWheelAmt());
				}

				for (Integer pressKey : carrier.getKeyPressCode()) {
					controlMouseRobot.keyPress(pressKey);
				}

				for (Integer releaseKey : carrier.getKeyReleaseCode()) {
					controlMouseRobot.keyRelease(releaseKey);
				}

				Dimension desktopSize = Toolkit.getDefaultToolkit().getScreenSize();
				BufferedImage curDesktop = controlMouseRobot.createScreenCapture(new Rectangle(desktopSize));
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				ImageIO.write(curDesktop, "jpg", out);
				ControlCarrier desktopState = new ControlCarrier();
				desktopState.setDesktopImg(out.toByteArray());

				response = new ObjectOutputStream(new BufferedOutputStream(client.getOutputStream()));
				response.writeObject(desktopState);
				response.flush();

			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} finally {

				if (request != null) {
					try {
						request.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				if (response != null) {
					try {
						response.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

	}

}