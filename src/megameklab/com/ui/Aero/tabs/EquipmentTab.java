/*
 * MegaMekLab - Copyright (C) 2008
 *
 * Original author - jtighe (torren@users.sourceforge.net)
 *
 * This program is  free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */

package megameklab.com.ui.Aero.tabs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.LocationFullException;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.WeaponType;
import megamek.common.weapons.ArtilleryWeapon;
import megameklab.com.ui.EntitySource;
import megameklab.com.util.CriticalTableModel;
import megameklab.com.util.EquipmentTableModel;
import megameklab.com.util.ITab;
import megameklab.com.util.RefreshListener;
import megameklab.com.util.UnitUtil;
import megameklab.com.util.XTableColumnModel;

public class EquipmentTab extends ITab implements ActionListener {

    /**
     *
     */
    private static final long serialVersionUID = 3978675469713289404L;

    private static final int T_ENERGY    =  0;
    private static final int T_BALLISTIC =  1;
    private static final int T_MISSILE   =  2;
    private static final int T_ARTILLERY =  3;
    private static final int T_PHYSICAL  =  4;
    private static final int T_WEAPON    =  5;
    private static final int T_AMMO      =  6;
    private static final int T_OTHER     =  7;
    private static final int T_NUM       =  8;


    private RefreshListener refresh;

    private JButton addButton = new JButton("Add");
    private JButton removeButton = new JButton("Remove");
    private JButton removeAllButton = new JButton("Remove All");
    private JComboBox<String> choiceType = new JComboBox<String>();
    private JTextField txtFilter = new JTextField();

    private JRadioButton rbtnStats = new JRadioButton("Stats");
    private JRadioButton rbtnFluff = new JRadioButton("Fluff");


    private TableRowSorter<EquipmentTableModel> equipmentSorter;

    private CriticalTableModel equipmentList;
    private EquipmentTableModel masterEquipmentList;
    private JTable masterEquipmentTable = new JTable();
    private JScrollPane masterEquipmentScroll = new JScrollPane();
    private JTable equipmentTable = new JTable();
    private JScrollPane equipmentScroll = new JScrollPane();

    private String ADD_COMMAND = "ADD";
    private String REMOVE_COMMAND = "REMOVE";
    private String REMOVEALL_COMMAND = "REMOVEALL";

    public static String getTypeName(int type) {
        switch(type) {
        case T_WEAPON:
            return "All Weapons";
        case T_ENERGY:
            return "Energy Weapons";
        case T_BALLISTIC:
            return "Ballistic Weapons";
        case T_MISSILE:
            return "Missile Weapons";
        case T_ARTILLERY:
            return "Artillery Weapons";
        case T_PHYSICAL:
            return "Physical Weapons";
        case T_AMMO:
            return "Ammunition";
        case T_OTHER:
            return "Other Equipment";
        default:
            return "?";
        }
    }

    public EquipmentTab(EntitySource eSource) {
        super(eSource);

        equipmentList = new CriticalTableModel(eSource.getEntity(), CriticalTableModel.WEAPONTABLE);
        equipmentTable.setModel(equipmentList);
        equipmentTable.setIntercellSpacing(new Dimension(0, 0));
        equipmentTable.setShowGrid(false);
        equipmentTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        equipmentTable.setDoubleBuffered(true);
        TableColumn column = null;
        for (int i = 0; i < equipmentList.getColumnCount(); i++) {
            column = equipmentTable.getColumnModel().getColumn(i);
            if(i == 0) {
                column.setPreferredWidth(200);
            }
            column.setCellRenderer(equipmentList.getRenderer());

        }
        equipmentScroll.setViewportView(equipmentTable);
        equipmentScroll.setMinimumSize(new java.awt.Dimension(300, 200));
        equipmentScroll.setPreferredSize(new java.awt.Dimension(300, 200));

        masterEquipmentList = new EquipmentTableModel(eSource.getEntity());
        masterEquipmentTable.setModel(masterEquipmentList);
        masterEquipmentTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        equipmentSorter = new TableRowSorter<EquipmentTableModel>(masterEquipmentList);
        equipmentSorter.setComparator(EquipmentTableModel.COL_HEAT, new WeaponIntegerSorter());
        equipmentSorter.setComparator(EquipmentTableModel.COL_MRANGE, new WeaponIntegerSorter());
        equipmentSorter.setComparator(EquipmentTableModel.COL_DAMAGE, new WeaponDamageSorter());
        equipmentSorter.setComparator(EquipmentTableModel.COL_RANGE, new WeaponRangeSorter());
        equipmentSorter.setComparator(EquipmentTableModel.COL_COST, new FormattedNumberSorter());
        masterEquipmentTable.setRowSorter(equipmentSorter);
        ArrayList<RowSorter.SortKey> sortKeys = new ArrayList<RowSorter.SortKey>();
        sortKeys.add(new RowSorter.SortKey(EquipmentTableModel.COL_NAME, SortOrder.ASCENDING));
        equipmentSorter.setSortKeys(sortKeys);
        XTableColumnModel equipColumnModel = new XTableColumnModel();
        masterEquipmentTable.setColumnModel(equipColumnModel);
        masterEquipmentTable.createDefaultColumnsFromModel();
        column = null;
        for (int i = 0; i < EquipmentTableModel.N_COL; i++) {
            column = masterEquipmentTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(masterEquipmentList.getColumnWidth(i));
            column.setCellRenderer(masterEquipmentList.getRenderer());
        }
        masterEquipmentTable.setIntercellSpacing(new Dimension(0, 0));
        masterEquipmentTable.setShowGrid(false);
        masterEquipmentTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        masterEquipmentTable.setDoubleBuffered(true);
        masterEquipmentScroll.setViewportView(masterEquipmentTable);

        masterEquipmentTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    JTable target = (JTable)e.getSource();
                    int view = target.getSelectedRow();
                    int selected = masterEquipmentTable.convertRowIndexToModel(view);
                    EquipmentType equip = masterEquipmentList.getType(selected);
                    addEquipment(equip);
                    fireTableRefresh();
                }
            }
        });

        masterEquipmentTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "add");
        masterEquipmentTable.getActionMap().put("add", new EnterAction());

        Enumeration<EquipmentType> miscTypes = EquipmentType.getAllTypes();
        ArrayList<EquipmentType> allTypes = new ArrayList<EquipmentType>();
        while (miscTypes.hasMoreElements()) {
            EquipmentType eq = miscTypes.nextElement();
            allTypes.add(eq);
        }

        masterEquipmentList.setData(allTypes);

        loadEquipmentTable();

        DefaultComboBoxModel<String> typeModel = new DefaultComboBoxModel<String>();
        for (int i = 0; i < T_NUM; i++) {
            typeModel.addElement(getTypeName(i));
        }
        choiceType.setModel(typeModel);
        choiceType.setSelectedIndex(0);
        choiceType.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterEquipment();
            }
        });

        txtFilter.setText("");
        txtFilter.setMinimumSize(new java.awt.Dimension(200, 28));
        txtFilter.setPreferredSize(new java.awt.Dimension(200, 28));
        txtFilter.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                filterEquipment();
            }
            public void insertUpdate(DocumentEvent e) {
                filterEquipment();
            }
            public void removeUpdate(DocumentEvent e) {
                filterEquipment();
            }
        });

        filterEquipment();
        addButton.setMnemonic('A');
        removeButton.setMnemonic('R');
        removeAllButton.setMnemonic('l');

        ButtonGroup bgroupView = new ButtonGroup();
        bgroupView.add(rbtnStats);
        bgroupView.add(rbtnFluff);

        rbtnStats.setSelected(true);
        rbtnStats.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setEquipmentView();
            }
        });
        rbtnFluff.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setEquipmentView();
            }
        });
        JPanel viewPanel = new JPanel(new GridLayout(0,2));
        viewPanel.add(rbtnStats);
        viewPanel.add(rbtnFluff);
        setEquipmentView();

        //layout
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();

        JPanel loadoutPanel = new JPanel(new GridBagLayout());
        JPanel databasePanel = new JPanel(new GridBagLayout());

        loadoutPanel.setBorder(BorderFactory.createTitledBorder("Current Loadout"));
        databasePanel.setBorder(BorderFactory.createTitledBorder("Equipment Database"));

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(2,2,2,2);
        gbc.fill = java.awt.GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        databasePanel.add(addButton, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.fill = java.awt.GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        databasePanel.add(choiceType, gbc);

        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.fill = java.awt.GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        databasePanel.add(txtFilter, gbc);

        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.fill = java.awt.GridBagConstraints.NONE;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        databasePanel.add(viewPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.fill = java.awt.GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        loadoutPanel.add(removeButton, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.fill = java.awt.GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        loadoutPanel.add(removeAllButton, gbc);

        gbc.insets = new Insets(2,0,0,0);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 4;
        gbc.fill = java.awt.GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        databasePanel.add(masterEquipmentScroll, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.fill = java.awt.GridBagConstraints.VERTICAL;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        loadoutPanel.add(equipmentScroll, gbc);

        setLayout(new BorderLayout());
        this.add(loadoutPanel, BorderLayout.WEST);
        this.add(databasePanel, BorderLayout.CENTER);

    }

    public void addRefreshedListener(RefreshListener l) {
        refresh = l;
    }

    private void loadEquipmentTable() {

        for (Mounted mount : getAero().getWeaponList()) {
            equipmentList.addCrit(mount);
        }

        for (Mounted mount : getAero().getAmmo()) {
            equipmentList.addCrit(mount);
        }

        List<EquipmentType> spreadAlreadyAdded = new ArrayList<EquipmentType>();

        for (Mounted mount : getAero().getMisc()) {

            EquipmentType etype = mount.getType();
            if (UnitUtil.isHeatSink(mount)
                    || etype.hasFlag(MiscType.F_JUMP_JET)
                    || etype.hasFlag(MiscType.F_JUMP_BOOSTER)
                    || etype.hasFlag(MiscType.F_TSM)
                    || etype.hasFlag(MiscType.F_INDUSTRIAL_TSM)
                    || (etype.hasFlag(MiscType.F_MASC) 
                            && !etype.hasSubType(MiscType.S_SUPERCHARGER))
                    || UnitUtil.isArmorOrStructure(etype)) {
                continue;
            }
            //if (UnitUtil.isUnitEquipment(mount.getType(), unit) || UnitUtil.isUn) {
                if (UnitUtil.isFixedLocationSpreadEquipment(etype) 
                        && !spreadAlreadyAdded.contains(etype)) {
                    equipmentList.addCrit(mount);
                    // keep track of spreadable equipment here, so it doesn't
                    // show up multiple times in the table
                    spreadAlreadyAdded.add(etype);
                } else {
                    equipmentList.addCrit(mount);
                }
            //}
        }


    }


    private void removeHeatSinks() {
        int location = 0;
        for (; location < equipmentList.getRowCount();) {

            Mounted mount = (Mounted) equipmentList.getValueAt(location, CriticalTableModel.EQUIPMENT);
            EquipmentType eq = mount.getType();
            if ((eq instanceof MiscType) && (UnitUtil.isHeatSink(mount))) {
                try {
                    equipmentList.removeCrit(location);
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    return;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                location++;
            }
        }
    }

    public void refresh() {
        removeAllListeners();
        filterEquipment();
        updateEquipment();
        addAllListeners();
        fireTableRefresh();
    }

    private void removeAllListeners() {
        addButton.removeActionListener(this);
        removeButton.removeActionListener(this);
        removeAllButton.removeActionListener(this);
    }

    private void addAllListeners() {
        addButton.addActionListener(this);
        removeButton.addActionListener(this);
        removeAllButton.addActionListener(this);
        addButton.setActionCommand(ADD_COMMAND);
        removeButton.setActionCommand(REMOVE_COMMAND);
        removeAllButton.setActionCommand(REMOVEALL_COMMAND);
        addButton.setMnemonic('A');
        removeButton.setMnemonic('R');
        removeAllButton.setMnemonic('L');
    }

    private void addEquipment(EquipmentType equip) {
        boolean success = false;
        Mounted mount = null;
        boolean isMisc = equip instanceof MiscType;
        if (isMisc && equip.hasFlag(MiscType.F_TARGCOMP)) {
            if (!UnitUtil.hasTargComp(getAero())) {
                mount = UnitUtil.updateTC(getAero(), equip);
                success = mount != null;
            }        
        } else {
            try {
                mount = new Mounted(getAero(), equip);
                getAero().addEquipment(mount, Entity.LOC_NONE, false);
                success = true;
            } catch (LocationFullException lfe) {
                // this can't happen, we add to Entity.LOC_NONE
            }
        }
        if (success) {
            equipmentList.addCrit(mount);
        }
    }

    public void actionPerformed(ActionEvent e) {

        if (e.getActionCommand().equals(ADD_COMMAND)) {
            int view = masterEquipmentTable.getSelectedRow();
            if(view < 0) {
                //selection got filtered away
                return;
            }
            int selected = masterEquipmentTable.convertRowIndexToModel(view);
            EquipmentType equip = masterEquipmentList.getType(selected);
            addEquipment(equip);
        } else if (e.getActionCommand().equals(REMOVE_COMMAND)) {
            int selectedRows[] = equipmentTable.getSelectedRows();
            for (Integer row : selectedRows){
                equipmentList.removeMounted(row);
            }
            equipmentList.removeCrits(selectedRows);
        } else if (e.getActionCommand().equals(REMOVEALL_COMMAND)) {
            removeAllEquipment();
        } else {
            return;
        }
        fireTableRefresh();
    }

    public void updateEquipment() {
        removeHeatSinks();
        equipmentList.removeAllCrits();
        loadEquipmentTable();
    }

    public void removeAllEquipment() {
        removeHeatSinks();
        for (int count = 0; count < equipmentList.getRowCount(); count++) {
            equipmentList.removeMounted(count);
        }
        equipmentList.removeAllCrits();
    }

    private void fireTableRefresh() {
        equipmentList.updateUnit(getAero());
        equipmentList.refreshModel();
        if (refresh != null) {
            refresh.refreshStatus();
            refresh.refreshBuild();
            refresh.refreshPreview();
        }
    }

    public CriticalTableModel getEquipmentList() {
        return equipmentList;
    }

    private void filterEquipment() {
        RowFilter<EquipmentTableModel, Integer> equipmentTypeFilter = null;
        final int nType = choiceType.getSelectedIndex();
        equipmentTypeFilter = new RowFilter<EquipmentTableModel,Integer>() {
            @Override
            public boolean include(Entry<? extends EquipmentTableModel, ? extends Integer> entry) {
                EquipmentTableModel equipModel = entry.getModel();
                EquipmentType etype = equipModel.getType(entry.getIdentifier());
                WeaponType wtype = null;
                if (etype instanceof WeaponType) {
                    wtype = (WeaponType)etype;
                }
                AmmoType atype = null;
                if (etype instanceof AmmoType) {
                    atype = (AmmoType)etype;
                }
                if (!UnitUtil.isLegal(getAero(), etype)) {
                    return false;
                }
                if (UnitUtil.isHeatSink(etype, true) || UnitUtil.isJumpJet(etype)) {
                    return false;
                }
                if ((etype instanceof MiscType) && (etype.hasFlag(MiscType.F_TSM) || etype.hasFlag(MiscType.F_INDUSTRIAL_TSM) || (etype.hasFlag(MiscType.F_MASC) && !etype.hasSubType(MiscType.S_SUPERCHARGER)))) {
                    return false;
                }
                Aero aero = getAero();
                if (((nType == T_OTHER) && UnitUtil.isAeroEquipment(etype, getAero()))
                        || (((nType == T_WEAPON) && (UnitUtil.isAeroWeapon(etype, aero) || UnitUtil.isPhysicalWeapon(etype))))
                        || ((nType == T_ENERGY) && UnitUtil.isAeroWeapon(etype, aero)
                            && (wtype != null) && (wtype.hasFlag(WeaponType.F_ENERGY)
                            || (wtype.hasFlag(WeaponType.F_PLASMA) && (wtype.getAmmoType() == AmmoType.T_PLASMA))))
                        || ((nType == T_BALLISTIC) && UnitUtil.isAeroWeapon(etype, aero)
                            && (wtype != null) && (wtype.hasFlag(WeaponType.F_BALLISTIC)
                                    && (wtype.getAmmoType() != AmmoType.T_NA)))
                        || ((nType == T_MISSILE) && UnitUtil.isAeroWeapon(etype, aero)
                            && (wtype != null) && ((wtype.hasFlag(WeaponType.F_MISSILE)
                                    && (wtype.getAmmoType() != AmmoType.T_NA)) || (wtype.getAmmoType() == AmmoType.T_C3_REMOTE_SENSOR)))
                        || ((nType == T_ARTILLERY) && UnitUtil.isAeroWeapon(etype, aero)
                            && (wtype != null) && (wtype instanceof ArtilleryWeapon))
                        || (((nType == T_AMMO) & (atype != null)) && UnitUtil.canUseAmmo(aero, atype))) {
                    if (txtFilter.getText().length() > 0) {
                        String text = txtFilter.getText();
                        return etype.getName().toLowerCase().contains(text.toLowerCase());
                    } else {
                        return true;
                    }
                }
                return false;
            }
        };
        equipmentSorter.setRowFilter(equipmentTypeFilter);
    }

    public void setEquipmentView() {
        XTableColumnModel columnModel = (XTableColumnModel)masterEquipmentTable.getColumnModel();
        if(rbtnStats.isSelected()) {
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_NAME), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_DAMAGE), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_DIVISOR), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_SPECIAL), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_HEAT), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_MRANGE), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_RANGE), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_SHOTS), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_TECH), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_TRATING), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_AVSL), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_AVSW), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_AVCL), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_AVDA), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_DINTRO), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_DEXTINCT), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_DREINTRO), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_COST), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_CREW), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_BV), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_TON), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_CRIT), true);
        } else {
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_NAME), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_DAMAGE), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_DIVISOR), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_SPECIAL), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_HEAT), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_MRANGE), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_RANGE), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_SHOTS), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_TECH), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_TRATING), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_AVSL), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_AVSW), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_AVCL), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_AVDA), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_DINTRO), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_DEXTINCT), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_DREINTRO), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_COST), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_CREW), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_BV), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_TON), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_CRIT), true);
        }
    }

    private class EnterAction extends AbstractAction {

        /**
         *
         */
        private static final long serialVersionUID = 8247993757008802162L;

        @Override
        public void actionPerformed(ActionEvent e) {
            int view = masterEquipmentTable.getSelectedRow();
            if(view < 0) {
                //selection got filtered away
                return;
            }
            int selected = masterEquipmentTable.convertRowIndexToModel(view);
            EquipmentType equip = masterEquipmentList.getType(selected);
            addEquipment(equip);
            fireTableRefresh();
        }
    }

    /**
     * A comparator for integers written as strings with "-" sorted to the bottom always
     * @author Jay Lawson
     *
     */
    public class WeaponIntegerSorter implements Comparator<String> {

        @Override
        public int compare(String s0, String s1) {
            if(s0.equals("-") && s1.equals("-")) {
                return 0;
            } else if(s0.equals("-")) {
                return 1;
            } else if(s1.equals("-")) {
                return -1;
            } else {
                //get the numbers associated with each string
                int r0 = Integer.parseInt(s0);
                int r1 = Integer.parseInt(s1);
                return ((Comparable<Integer>)r1).compareTo(r0);
            }
        }
    }

    /**
     * A comparator for integers written as strings with "-" sorted to the bottom always
     * @author Jay Lawson
     *
     */
    public class WeaponRangeSorter implements Comparator<String> {

        @Override
        public int compare(String s0, String s1) {
            return s0.compareTo(s1);
        }
    }

    public class WeaponDamageSorter implements Comparator<String> {

        @Override
        public int compare(String s0, String s1) {
            if(s0.equals("-") && s1.equals("-")) {
                return 0;
            } else if(s0.equals("-")) {
                return 1;
            } else if(s1.equals("-")) {
                return -1;
            } else if(s0.equals("Cluster") && s1.equals("-")) {
                return 1;
            } else if(s0.equals("-") && s1.equals("Cluster")) {
                return -1;
            } else if(s0.equals("Cluster") && s1.equals("Special")) {
                return 1;
            } else if(s0.equals("Special") && s1.equals("Cluster")) {
                return -1;
            } else if(s0.equals("Special") && s1.equals("-")) {
                return 1;
            } else if(s0.equals("-") && s1.equals("Special")) {
                return -1;
            } else if(s0.equals("Cluster") && s1.equals("Cluster")) {
                return 0;
            } else if(s0.equals("Cluster")) {
                return 1;
            } else if(s1.equals("Cluster")) {
                return -1;
            } else if(s0.equals("Special") && s1.equals("Special")) {
                return 0;
            } else if(s0.equals("Special")) {
                return 1;
            } else if(s1.equals("Special")) {
                return -1;
            } else {
                //get the numbers associated with each string
                int r1 = parseDamage(s1);
                int r0 = parseDamage(s0);
                return ((Comparable<Integer>)r1).compareTo(r0);
            }
        }

        private int parseDamage(String s) {
            int damage = 0;
            if(s.contains("/")) {
                damage = Integer.parseInt(s.split("/")[0]);
            } else {
                damage = Integer.parseInt(s);
            }
            return damage;
        }
    }

    /**
     * A comparator for numbers that have been formatted with DecimalFormat
     * @author Jay Lawson
     *
     */
    public class FormattedNumberSorter implements Comparator<String> {

        @Override
        public int compare(String s0, String s1) {
            //lets find the weight class integer for each name
            DecimalFormat format = new DecimalFormat();
            int l0 = 0;
            try {
                l0 = format.parse(s0).intValue();
            } catch (java.text.ParseException e) {
                e.printStackTrace();
            }
            int l1 = 0;
            try {
                l1 = format.parse(s1).intValue();
            } catch (java.text.ParseException e) {
                e.printStackTrace();
            }
            return ((Comparable<Integer>)l0).compareTo(l1);
        }
    }
}
