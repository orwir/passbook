package home.ingvar.passbook.ui;

import home.ingvar.passbook.dao.DaoFactory;
import home.ingvar.passbook.dao.ItemDAO;
import home.ingvar.passbook.dao.ResultException;
import home.ingvar.passbook.dao.UserDAO;
import home.ingvar.passbook.lang.Exceptions;
import home.ingvar.passbook.lang.Labels;
import home.ingvar.passbook.transfer.User;
import home.ingvar.passbook.ui.dialogs.Dialog;
import home.ingvar.passbook.ui.res.IMG;
import home.ingvar.passbook.ui.views.InstallPanel;
import home.ingvar.passbook.ui.views.LoginPanel;
import home.ingvar.passbook.ui.views.MainPanel;
import home.ingvar.passbook.ui.views.ProfilePanel;
import home.ingvar.passbook.ui.views.RegPanel;
import home.ingvar.passbook.ui.views.SettingsPanel;
import home.ingvar.passbook.utils.I18n;
import home.ingvar.passbook.utils.LOG;
import home.ingvar.passbook.utils.PROPS;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Locale;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class MainFrame extends JFrame {

	private static final long serialVersionUID = 1L;
	private static final int DEFAULT_WIDTH = 640;
	private static final int DEFAULT_HEIGHT = 480;
	
	private final PROPS properties;
	private final I18n i18n;
	private final Menu menu;
	
	private AbstractPanel prevView;
	private AbstractPanel view; //current view
	private User user; //current user
	
	private DaoFactory daoFactory;
	private UserDAO userDAO;
	private ItemDAO itemDAO;
	
	public MainFrame() {
		properties = PROPS.getInstance();
		i18n = I18n.getInstance();
		i18n.setLocale(properties.getLang());
		
		menu = new Menu();
		setJMenuBar(menu.getBar());
		
		//set theme
		Theme theme = null;
		try {
			theme = Theme.valueOf(properties.getTheme());
		} catch(IllegalArgumentException e) {
			theme = Theme.STANDART;
			properties.setTheme(theme.toString());
			LOG.warn(i18n.get(Labels.TITLE_WARNING), i18n.getException(Exceptions.THEME_FAIL), e);
		}
		//chose view
		Form form = null;
		try {
			int db = properties.getDB();
			daoFactory = DaoFactory.newInstance(db);
			userDAO = daoFactory.getUserDAO();
			itemDAO = daoFactory.getItemDAO();
			form = daoFactory.test() ? Form.LOGIN : Form.INSTALL;
		} catch(InstantiationException e) {
			LOG.error(i18n.get(Labels.TITLE_ERROR), i18n.getException(Exceptions.STORAGE_FAIL), e);
			form = Form.INSTALL;
		}
		//update DB if it needed
		if(Form.LOGIN.equals(form)) {
			try {
				daoFactory.update();
			} catch (ResultException e) {
				LOG.error(i18n.get(Labels.TITLE_ERROR), i18n.getException(Exceptions.STORAGE_FAIL), e);
				throw new RuntimeException(e); //TODO: show error
			}
		}
		Dialog.initialize(this);
		setPreference();
		createMenu();
		createForms();
		nextView(form);
		setTheme(theme);
		updateI18n();
	}
	
	public DaoFactory getDaoFactory() {
		return daoFactory;
	}
	
	public UserDAO getUserDAO() {
		return userDAO;
	}
	
	public ItemDAO getItemDAO() {
		return itemDAO;
	}
	
	public User getUser() {
		return user;
	}
	
	public void setUser(User user) {
		this.user = user;
		updateTitle();
	}
	
	public void nextView(Form form) {
		if(view != null) {
			remove(view);
			prevView = view;
		}
		view = form.getPanel();
		view.preShow();
		view.updateI18n();
		getRootPane().setDefaultButton(view.getDefaultButton());
		add(view);
		view.revalidate();
		repaint();
	}
	
	public void prevView() {
		if(prevView != null) {
			AbstractPanel tmp = null;
			if(view != null) {
				remove(view);
				tmp = view;
			}
			view = prevView;
			prevView = tmp;
			view.preShow();
			view.updateI18n();
			getRootPane().setDefaultButton(view.getDefaultButton());
			add(view);
			view.revalidate();
			repaint();
		}
	}
	
	public void logout() {
		daoFactory.close();
		setUser(null);
		nextView(Form.LOGIN);
	}
	
	public void setStorage(DaoFactory factory, int id) {
		properties.setDB(id);
		daoFactory = factory;
		userDAO = daoFactory.getUserDAO();
		itemDAO = daoFactory.getItemDAO();
	}
	
	public void setProgramLocale(Locale locale) {
		i18n.setLocale(locale);
		menu.chose(locale.getLanguage());
		properties.setLang(locale.getLanguage());
		updateI18n();
		if(view.equals(Form.SETTINGS.getPanel())) { //update settings on panel
			view.preShow();
		}
	}
	
	public void setTheme(Theme theme) {
		try {
			UIManager.setLookAndFeel(theme.getClassName());
			properties.setTheme(theme.toString());
			SwingUtilities.updateComponentTreeUI(this);
			menu.updateMenuStyle();
			for(@SuppressWarnings("rawtypes") AbstractDialog d : Dialog.getDialogs()) {
				SwingUtilities.updateComponentTreeUI(d);
			}
			//and update all views
			for(Form form : Form.values()) {
				SwingUtilities.updateComponentTreeUI(form.getPanel());
			}
			menu.chose(theme.toString());
			properties.setTheme(theme.toString());
			if(view.equals(Form.SETTINGS.getPanel())) { //update settings on panel
				view.preShow();
			}
		} catch(Exception e) {
			LOG.error(i18n.get(Labels.TITLE_ERROR), e.getMessage(), e);
		}
	}
	
	private void setPreference() {
		Toolkit tk = Toolkit.getDefaultToolkit();
		Dimension screen = tk.getScreenSize();
		
		setTitle(i18n.get(Labels.TITLE_MAIN));
		setIconImage(IMG.FAVICON.getImage());
		setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
		setLocation((screen.width - DEFAULT_WIDTH) / 2, (screen.height - DEFAULT_HEIGHT) / 2);
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				close();				
			}
		});
	}
	
	private void createMenu() {
		JMenu fileMenu = menu.addMenu(Labels.MENU_FILE);
		menu.addMenuItem(fileMenu, Labels.MENU_FILE_EXIT).addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				close();
			}
		});
		
		JMenu settingsMenu = menu.addMenu(Labels.MENU_SETTINGS);
		JMenu langMenu = menu.addMenu(settingsMenu, Labels.MENU_SETTINGS_LANG);
		for(final Locale l : I18n.getAvailable()) {
			String lbl = l.getDisplayName(l);
			lbl = lbl.substring(0, 1).toUpperCase() + lbl.substring(1);
			final JMenuItem item = menu.addMenuItemUnilocale(langMenu, lbl);
			item.setName(l.getLanguage());
			if(l.equals(i18n.getLocale())) {
				menu.chose(item);
			}
			item.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					setProgramLocale(l);
				}
			});
		}
		
		JMenu themeMenu = menu.addMenu(settingsMenu, Labels.MENU_SETTINGS_THEME);
		Theme currentTheme = Theme.valueOf(properties.getTheme());
		for(final Theme t : Theme.values()) {
			final JMenuItem item = menu.addMenuItem(themeMenu, t.getI18nName());
			item.setName(t.toString());
			if(t.equals(currentTheme)) {
				menu.chose(item);
			}
			item.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					setTheme(t);
				}
			});
		}
		
		settingsMenu.addSeparator();
		menu.addMenuItem(settingsMenu, Labels.MENU_SETTINGS).addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				nextView(Form.SETTINGS);
			}
		});
		
		JMenu aboutMenu = menu.addMenu(Labels.MENU_HELP);
		menu.addMenuItem(aboutMenu, Labels.MENU_HELP_ABOUT).addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Dialog.getAboutDialog().showDialog();
			}
		});
	}
	
	private void createForms() {
		Form.INSTALL.setPanel(new InstallPanel().inject(this).postConstruct());
		Form.REGISTER.setPanel(new RegPanel().inject(this).postConstruct());
		Form.LOGIN.setPanel(new LoginPanel().inject(this).postConstruct());
		Form.MAIN.setPanel(new MainPanel().inject(this).postConstruct());
		Form.PROFILE.setPanel(new ProfilePanel().inject(this).postConstruct());
		Form.SETTINGS.setPanel(new SettingsPanel().inject(this).postConstruct());
	}
	
	private void updateTitle() {
		String title = i18n.get(Labels.TITLE_MAIN);
		if(user != null) {
			title += " - " + ((user.getFullname() == null || user.getFullname().isEmpty()) ? user.getUsername() : user.getFullname());
		}
		setTitle(title);
	}
	
	private void updateI18n() {
		updateTitle();
		menu.updateI18n();
		view.updateI18n();
		for(@SuppressWarnings("rawtypes") AbstractDialog d : Dialog.getDialogs()) {
			d.updateI18n();
		}
	}
	
	private void close() {
		if(properties.isChanged()) {
			properties.saveProperties();
		}
		dispose();
		System.exit(0);
	}

}
