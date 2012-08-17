package home.ingvar.passbook.ui.views;

import home.ingvar.passbook.dao.ResultException;
import home.ingvar.passbook.lang.Labels;
import home.ingvar.passbook.transfer.Item;
import home.ingvar.passbook.ui.AbstractPanel;
import home.ingvar.passbook.ui.Form;
import home.ingvar.passbook.ui.ItemsTableModel;
import home.ingvar.passbook.ui.MainFrame;
import home.ingvar.passbook.ui.res.IMG;
import home.ingvar.passbook.utils.LOG;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;

public class MainPanel extends AbstractPanel {

	private static final long serialVersionUID = 1L;

	private ItemsTableModel model;
	private JTable table;
	private TableRowSorter<ItemsTableModel> sorter;
	private JLabel lblStatus;
	private JLabel lblService;
	private JTextField fldService;
	private JButton btnAdd;
	private JButton btnEdit;
	private JButton btnDelete;
	private JButton btnProfile;
	private JButton btnLogout;
	
	private Action actAdd;
	private Action actEdit;
	private Action actDelete;
	
	private ItemDialog dialog;
	
	public MainPanel(MainFrame frame) {
		super(frame);
		model  = new ItemsTableModel(getItemDAO());
		table  = new JTable(model);
		sorter = new TableRowSorter<ItemsTableModel>(model);
		lblStatus = new StatusLabel();
		lblService = new JLabel();
		fldService = new JTextField(15);
		actAdd = new ViewItemAction("", new ImageIcon(IMG.ADD_ITEM.getImage()), true);
		actEdit = new ViewItemAction("", new ImageIcon(IMG.EDIT_ITEM.getImage()), false);
		actDelete = new DeleteItemsAction("", new ImageIcon(IMG.DELETE_ITEM.getImage()));
		btnAdd = new JButton(actAdd);
		btnEdit = new JButton(actEdit);
		btnDelete = new JButton(actDelete);
		btnProfile = new JButton(new ImageIcon(IMG.USER.getImage()));
		btnLogout = new JButton(new ImageIcon(IMG.EXIT.getImage()));
		dialog = new ItemDialog(frame);
		
		btnProfile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				show(Form.PROFILE);
			}
		});
		btnLogout.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				getRoot().logout();
			}
		});
		composition();
	}
	
	
	@Override
	protected void init() {
		fldService.setText("");
		try {
			List<Item> items = getItemDAO().list(getUser());
			model.loadItems(items);
		} catch(ResultException e) {
			LOG.error(getText(Labels.TITLE_ERROR), e.getMessage(), e);
		}
	}

	@Override
	protected void updateI18n() {
		lblService.setText(getText(Labels.LABELS_SERVICE) + ":");
		actAdd.putValue(Action.NAME, getText(Labels.BUTTONS_ITEM_ADD));
		actEdit.putValue(Action.NAME, getText(Labels.BUTTONS_ITEM_EDIT));
		actDelete.putValue(Action.NAME, getText(Labels.BUTTONS_ITEM_DELETE));
		btnAdd.setText("");
		btnEdit.setText("");
		btnDelete.setText("");
		dialog.updateI18n();
		for(int i = 0; i < table.getColumnCount(); i++) {
			table.getColumnModel().getColumn(i).setHeaderValue(model.getColumnName(i));
		}
		repaint();
	}
	
	private void filter(String expression) {
		if(model.getRowCount() > 0) {
			RowFilter<ItemsTableModel, Object> rf = RowFilter.regexFilter(expression, 0);
			sorter.setRowFilter(rf);
		}
	}
	
	private void composition() {
		setLayout(new BorderLayout());
		add(new JScrollPane(table), BorderLayout.CENTER);
		
		sorter.setSortsOnUpdates(true);
		table.setRowSorter(sorter);
		table.setAutoscrolls(true);
		table.getTableHeader().setReorderingAllowed(false); //disable moving column
		
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent event) {
				int row = table.rowAtPoint(event.getPoint());
				int col = table.columnAtPoint(event.getPoint());
				if(event.getButton() == MouseEvent.BUTTON3) {
					table.setRowSelectionInterval(row, row);
				}
				model.showPassword(-1);
				if(event.getButton() == MouseEvent.BUTTON1 && col == 2) { //password column
					int modelRow = table.convertRowIndexToModel(row);
					model.showPassword(modelRow);
				}
			}
			@Override
			public void mouseClicked(MouseEvent event) {
				if(event.getButton() == MouseEvent.BUTTON1 && event.getClickCount() >= 2) {
					int row = table.convertRowIndexToModel(table.rowAtPoint(event.getPoint()));
					Item item = model.getItem(row);
					StringSelection data = new StringSelection(item.getPassword());
					Clipboard clipboard  = Toolkit.getDefaultToolkit().getSystemClipboard();
					clipboard.setContents(data, null);
					lblStatus.setText(getText(Labels.MESSAGES_COPY_PASSWORD));
				}
			}
		});
		
		FlowLayout layout = new FlowLayout(FlowLayout.RIGHT);
		layout.setVgap(2);
		JPanel statusPanel = new JPanel(layout);
		add(statusPanel, BorderLayout.SOUTH);
		statusPanel.add(lblStatus);
		statusPanel.setPreferredSize(new Dimension(getRoot().getWidth(), lblStatus.getFont().getSize() + 10));
		
		JPanel wrapControls = new JPanel(new BorderLayout());
		add(wrapControls, BorderLayout.NORTH);
		
		JPanel itemControls = new JPanel(new FlowLayout(FlowLayout.LEFT));
		wrapControls.add(itemControls, BorderLayout.WEST);
		
		itemControls.add(btnAdd);
		itemControls.add(btnEdit);
		itemControls.add(btnDelete);
		
		itemControls.add(lblService);
		fldService.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent event) {
				filter(fldService.getText());
			}
			@Override
			public void insertUpdate(DocumentEvent event) {
				filter(fldService.getText());
			}
			@Override
			public void changedUpdate(DocumentEvent event) {
				filter(fldService.getText());
			}
		});
		itemControls.add(fldService);
		
		JPanel userControls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		wrapControls.add(userControls);
		userControls.add(btnProfile, BorderLayout.EAST);
		userControls.add(btnLogout);
		
		JPopupMenu popup = new JPopupMenu();
		popup.add(new JMenuItem(actAdd));
		popup.add(new JMenuItem(actEdit));
		popup.add(new JMenuItem(actDelete));
		table.setComponentPopupMenu(popup);
	}
	
	// ------------- INNER CLASSES ------------- //
	
	private class StatusLabel extends JLabel {
		private static final long serialVersionUID = 1L;
		private static final int TIMEOUT = 5000;
		private Runnable cleaner;
		
		public StatusLabel() {
			cleaner = new Runnable() {
				@Override
				public void run() {
					try {Thread.sleep(TIMEOUT);} catch(InterruptedException e) {}
					setText("");
				}
			};
		}
		
		@Override
		public void setText(String text) {
			super.setText(text);
			if(!text.isEmpty()) {
				new Thread(cleaner).start();
			}
		}
	}
	
	
	
	private class ViewItemAction extends AbstractAction {
		private static final long serialVersionUID = 1L;
		private boolean isNew;
		
		public ViewItemAction(String name, Icon icon, boolean isNew) {
			super(name, icon);
			this.isNew = isNew;
		}
		
		@Override
		public void actionPerformed(ActionEvent event) {
			if(isNew) {
				add();
			} else {
				edit();
			}
		}
		
		private void add() {
			Item item = dialog.showDialog(getUser());
			if(item != null) {
				try {
					model.addItem(item);
				} catch(ResultException e) {
					LOG.error(getText(Labels.TITLE_ERROR), e.getMessage(), e);
				}
			}
		}
		
		private void edit() {
			int viewRow = table.getSelectedRow();
			if(viewRow == -1) {
				LOG.warn(getText(Labels.TITLE_WARNING), getText(Labels.MESSAGES_ITEM_NOT_SELECT), null);
				return;
			}
			int row = table.convertRowIndexToModel(viewRow);
			Item item = dialog.showDialog(model.getItem(row).clone());
			if(item != null) {
				try {
					model.updateItem(item);
				} catch(ResultException e) {
					LOG.error(getText(Labels.TITLE_ERROR), e.getMessage(), e);
				}
			}
		}
	}
	
	
	
	private class DeleteItemsAction extends AbstractAction {
		private static final long serialVersionUID = 1L;
		
		public DeleteItemsAction(String name, Icon icon) {
			super(name, icon);
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			int rows[] = table.getSelectedRows();
			if(rows.length == 0) {
				LOG.warn(getText(Labels.TITLE_WARNING), getText(Labels.MESSAGES_ITEM_NOT_SELECT), null);
				return;
			}
			try {
				for(int i = 0; i < rows.length; i++) {
					int row = table.convertRowIndexToModel(rows[i]);
					model.removeItem(row);
				}
				model.fireTableRowsDeleted(0, table.getRowCount() - 1);
			} catch(ResultException e) {
				LOG.error(getText(Labels.TITLE_ERROR), e.getMessage(), e);
			}
		}
		
	}

}
