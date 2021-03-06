/*
 * Copyright 2007-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.ymate.platform.persistence.jdbc.scaffold;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import net.ymate.platform.core.YMP;
import net.ymate.platform.core.lang.BlurObject;
import net.ymate.platform.core.util.ClassUtils;
import net.ymate.platform.core.util.RuntimeUtils;
import net.ymate.platform.persistence.base.EntityMeta;
import net.ymate.platform.persistence.jdbc.*;
import net.ymate.platform.persistence.jdbc.base.IResultSetHandler;
import net.ymate.platform.persistence.jdbc.query.SQL;
import net.ymate.platform.persistence.jdbc.support.ResultSetHelper;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Date;

/**
 * 持久层代码生成脚手架程序，通过已有数据库表结构逆向生成Java代码(create at 2013年9月22日下午9:44:09)
 *
 * @author 刘镇 (suninformation@163.com) on 15/12/30 下午9:30
 * @version 1.0
 */
public class EntityGenerator {

    private static final Log _LOG = LogFactory.getLog(EntityGenerator.class);

    private String __templateRootPath = EntityGenerator.class.getPackage().getName().replace(".", "/");
    private Configuration __freemarkerConfig;

    private YMP __owner;
    private IDatabase __jdbc;

    private IEntityNamedFilter __namedFilter;

    private List<String> __readonlyFields;

    private EntityGenerator() {
        __freemarkerConfig = new Configuration(Configuration.VERSION_2_3_22);
        __freemarkerConfig.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
        __freemarkerConfig.setClassForTemplateLoading(EntityGenerator.class, "/");
        __freemarkerConfig.setDefaultEncoding("UTF-8");
    }

    public EntityGenerator(YMP owner) {
        this();
        if (owner == null) {
            owner = YMP.get();
        }
        this.__owner = owner;
        this.__jdbc = JDBC.get(__owner);
        //
        __namedFilter = ClassUtils.impl(__owner.getConfig().getParam("jdbc.named_filter_class"), IEntityNamedFilter.class, this.getClass());
        //
        __readonlyFields = Arrays.asList(StringUtils.split(StringUtils.trimToEmpty(__owner.getConfig().getParam("jdbc.readonly_field_list")).toLowerCase(), '|'));
    }

    /**
     * @param dbName     数据库名称
     * @param dbUserName 所属用户名称
     * @param tableName  表名称
     * @param view       指定当前查询的是视图
     * @return 获取数据表元数据描述对象
     */
    private TableMeta getTableMeta(String dbName, String dbUserName, String tableName, boolean view) {
        IConnectionHolder _connHolder = null;
        Statement _statement = null;
        ResultSet _resultSet = null;
        Map<String, ColumnInfo> _tableFields = new LinkedHashMap<String, ColumnInfo>();
        List<String> _pkFields = new LinkedList<String>();
        TableMeta _meta = new TableMeta(_pkFields, _tableFields);
        try {
            _connHolder = __jdbc.getDefaultConnectionHolder();
            String _dbType = _connHolder.getDialect().getName();
            DatabaseMetaData _dbMetaData = _connHolder.getConnection().getMetaData();
            System.out.println(">>> Catalog: " + dbName);
            System.out.println(">>> Schema: " + dbUserName);
            System.out.println(">>> Table: " + tableName);
            if (!view) {
                _resultSet = _dbMetaData.getPrimaryKeys(dbName, _dbType.equalsIgnoreCase("oracle") ? dbUserName.toUpperCase() : dbUserName, tableName);
                if (_resultSet == null) {
                    System.err.println("Database table \"" + tableName + "\" primaryKey resultSet is null, ignored");
                    return null;
                } else {
                    while (_resultSet.next()) {
                        _pkFields.add(_resultSet.getString(4).toLowerCase());
                    }
                    if (_pkFields.isEmpty()) {
                        System.err.println("Database table \"" + tableName + "\" does not set the primary key, ignored");
                        return null;
                    }
                }
            }
            //
            //
            System.out.println(">>> " + "COLUMN_NAME / " +
                    "COLUMN_CLASS_NAME / " +
                    "PRIMARY_KEY / " +
                    "AUTO_INCREMENT / " +
                    "SIGNED / " +
                    "PRECISION / " +
                    "SCALE / " +
                    "NULLABLE / " +
                    "DEFAULT / " +
                    "REMARKS");
            //
            _statement = _connHolder.getConnection().createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            _resultSet = _statement.executeQuery("SELECT * FROM ".concat(_connHolder.getDialect().wrapIdentifierQuote(tableName)));
            ResultSetMetaData _rsMetaData = _resultSet.getMetaData();
            //
            for (int _idx = 1; _idx <= _rsMetaData.getColumnCount(); _idx++) {
                // 获取字段元数据对象
                ResultSet _column = _dbMetaData.getColumns(dbName, _dbType.equalsIgnoreCase("oracle") ? dbUserName.toUpperCase() : dbUserName, tableName, _rsMetaData.getColumnName(_idx));
                if (_column.next()) {
                    // 提取字段定义及字段默认值
                    _tableFields.put(_rsMetaData.getColumnName(_idx).toLowerCase(), new ColumnInfo(
                            __namedFilter,
                            _rsMetaData.getColumnName(_idx).toLowerCase(),
                            _rsMetaData.getColumnClassName(_idx),
                            _rsMetaData.isAutoIncrement(_idx),
                            _rsMetaData.isSigned(_idx),
                            _rsMetaData.getPrecision(_idx),
                            _rsMetaData.getScale(_idx),
                            _rsMetaData.isNullable(_idx),
                            _column.getString("COLUMN_DEF"),
                            _column.getString("REMARKS")));
                    System.out.println("--> " + _rsMetaData.getColumnName(_idx).toLowerCase() + "\t" +
                            _rsMetaData.getColumnClassName(_idx) + "\t" +
                            _pkFields.contains(_rsMetaData.getColumnName(_idx).toLowerCase()) + "\t" +
                            _rsMetaData.isAutoIncrement(_idx) + "\t" +
                            _rsMetaData.isSigned(_idx) + "\t" +
                            _rsMetaData.getPrecision(_idx) + "\t" +
                            _rsMetaData.getScale(_idx) + "\t" +
                            _rsMetaData.isNullable(_idx) + "\t" +
                            _column.getString("COLUMN_DEF") + "\t" +
                            _column.getString("REMARKS"));
                }
                _column.close();
            }
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        } finally {
            if (_statement != null) {
                try {
                    _statement.close();
                } catch (SQLException e) {
                    _LOG.warn("", e);
                }
            }
            if (_resultSet != null) {
                try {
                    _resultSet.close();
                } catch (SQLException e) {
                    _LOG.warn("", e);
                }
            }
            if (_connHolder != null) {
                _connHolder.release();
            }
        }
        return _meta;
    }

    /**
     * @return 获取数据库表名称集合
     */
    private List<String> getTableNames() {
        try {
            return __jdbc.openSession(new ISessionExecutor<List<String>>() {
                @Override
                public List<String> execute(ISession session) throws Exception {
                    String _dbType = session.getConnectionHolder().getDialect().getName();
                    String _sql = null;
                    if ("mysql".equalsIgnoreCase(_dbType)) {
                        _sql = "show full tables where Table_type='BASE TABLE'";
                    } else if ("oracle".equalsIgnoreCase(_dbType)) {
                        _sql = "select t.table_name from user_tables t";
                    } else if ("sqlserver".equalsIgnoreCase(_dbType)) {
                        _sql = "select name from sysobjects where xtype='U'";
                    } else {
                        throw new Error("The current database \"" + _dbType + "\" type not supported");
                    }
                    final List<String> _results = new ArrayList<String>();
                    ResultSetHelper _helper = ResultSetHelper.bind(session.find(SQL.create(_sql), IResultSetHandler.ARRAY));
                    _helper.forEach(new ResultSetHelper.ItemHandler() {
                        public boolean handle(ResultSetHelper.ItemWrapper wrapper, int row) throws Exception {
                            _results.add(wrapper.getAsString(0));
                            return true;
                        }
                    });
                    return _results;
                }
            });
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * @return 获取数据库视图名称集合
     */
    private List<String> getViewNames() {
        try {
            return __jdbc.openSession(new ISessionExecutor<List<String>>() {
                @Override
                public List<String> execute(ISession session) throws Exception {
                    String _dbType = session.getConnectionHolder().getDialect().getName();
                    String _sql = null;
                    if ("mysql".equalsIgnoreCase(_dbType)) {
                        _sql = "show full tables where Table_type='VIEW'";
                    } else if ("oracle".equalsIgnoreCase(_dbType)) {
                        _sql = "select view_name from user_views";
                    } else if ("sqlserver".equalsIgnoreCase(_dbType)) {
                        _sql = "select name from sysobjects where xtype='V'";
                    } else {
                        throw new Error("The current database \"" + _dbType + "\" type not supported");
                    }
                    final List<String> _results = new ArrayList<String>();
                    ResultSetHelper _helper = ResultSetHelper.bind(session.find(SQL.create(_sql), IResultSetHandler.ARRAY));
                    _helper.forEach(new ResultSetHelper.ItemHandler() {
                        public boolean handle(ResultSetHelper.ItemWrapper wrapper, int row) throws Exception {
                            _results.add(wrapper.getAsString(0));
                            return true;
                        }
                    });
                    return _results;
                }
            });
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据数据库表定义创建实体类文件
     *
     * @param view 指定当前创建的是视图
     */
    public void createEntityClassFiles(boolean view) {
        String _dbName = __owner.getConfig().getParam("jdbc.db_name");
        String _dbUser = __owner.getConfig().getParam("jdbc.db_username");
        String[] _prefixs = StringUtils.split(StringUtils.trimToEmpty(__owner.getConfig().getParam("jdbc.table_prefix")), '|');
        boolean _isRemovePrefix = new BlurObject(__owner.getConfig().getParam("jdbc.remove_table_prefix")).toBooleanValue();
        List<String> _tableExcludeList = Arrays.asList(StringUtils.split(StringUtils.trimToEmpty(__owner.getConfig().getParam("jdbc.table_exclude_list")).toLowerCase(), "|"));
        //
        List<String> _tableList = Arrays.asList(StringUtils.split(StringUtils.trimToEmpty(__owner.getConfig().getParam("jdbc.table_list")), "|"));
        if (_tableList.isEmpty()) {
            if (view) {
                _tableList = getViewNames();
            } else {
                _tableList = getTableNames();
            }
        }
        for (String _tableName : _tableList) {
            if (checkTableNameBlacklist(_tableExcludeList, _tableName)) {
                buildEntityClassFile(getTableMeta(_dbName, _dbUser, _tableName, view), _tableName, _prefixs, _isRemovePrefix, view);
            }
        }
    }

    private boolean checkTableNameBlacklist(List<String> _tableExcludeList, String _tableName) {
        // 判断黑名单
        if (!_tableExcludeList.isEmpty()) {
            if (_tableExcludeList.contains(_tableName.toLowerCase())) {
                return false;
            } else {
                boolean _flag = false;
                for (String _excludedName : _tableExcludeList) {
                    if (StringUtils.contains(_excludedName, "*") && StringUtils.startsWithIgnoreCase(_tableName, StringUtils.substringBefore(_excludedName, "*"))) {
                        _flag = true;
                        break;
                    }
                }
                if (_flag) {
                    return false;
                }
            }
        }
        return true;
    }

    private String __doNamedFilter(String original) {
        if (__namedFilter != null) {
            return StringUtils.defaultIfBlank(__namedFilter.doFilter(original), original);
        }
        return original;
    }

    private void buildEntityClassFile(TableMeta _tableMeta, String _tableName, String[] _prefixs, boolean _isRemovePrefix, boolean view) {
        if (_tableMeta != null) {
            Map<String, Object> _propMap = buildPropMap();
            String _modelName = null;
            for (String _prefix : _prefixs) {
                if (_tableName.startsWith(_prefix)) {
                    if (_isRemovePrefix) {
                        _tableName = _tableName.substring(_prefix.length());
                    }
                    _modelName = StringUtils.capitalize(EntityMeta.propertyNameToFieldName(__doNamedFilter(_tableName)));
                    break;
                }
            }
            if (StringUtils.isBlank(_modelName)) {
                _modelName = StringUtils.capitalize(EntityMeta.propertyNameToFieldName(__doNamedFilter(_tableName)));
            }
            //
            _propMap.put("tableName", _tableName);
            _propMap.put("modelName", _modelName);
            List<Attr> _fieldList = new ArrayList<Attr>(); // 用于完整的构造方法
            List<Attr> _fieldListForNotNullable = new ArrayList<Attr>(); // 用于非空字段的构造方法
            List<Attr> _allFieldList = new ArrayList<Attr>(); // 用于生成字段名称常量
            if (_tableMeta.getPkSet().size() > 1) {
                _propMap.put("primaryKeyType", _modelName + "PK");
                _propMap.put("primaryKeyName", StringUtils.uncapitalize((String) _propMap.get("primaryKeyType")));
                List<Attr> _primaryKeyList = new ArrayList<Attr>();
                _propMap.put("primaryKeyList", _primaryKeyList);
                Attr _pkAttr = new Attr((String) _propMap.get("primaryKeyType"),
                        (String) _propMap.get("primaryKeyName"), null, false, false, 0, 0, 0, null, null);
                _fieldList.add(_pkAttr);
                _fieldListForNotNullable.add(_pkAttr);
                //
                for (String pkey : _tableMeta.getPkSet()) {
                    ColumnInfo _ci = _tableMeta.getFieldMap().get(pkey);
                    Attr _attr = _ci.toAttr();
                    if (__readonlyFields.contains(_attr.getColumnName().toLowerCase())) {
                        _attr.setReadonly(true);
                    }
                    _primaryKeyList.add(_attr);
                    _allFieldList.add(new Attr("String",
                            __doNamedFilter(_ci.getColumnName()).toUpperCase(),
                            _ci.getColumnName(),
                            _ci.isAutoIncrement(),
                            _ci.isSigned(),
                            _ci.getPrecision(),
                            _ci.getScale(),
                            _ci.getNullable(),
                            _ci.getDefaultValue(),
                            _ci.getRemarks()));
                }
                for (String key : _tableMeta.getFieldMap().keySet()) {
                    if (_tableMeta.getPkSet().contains(key)) {
                        continue;
                    }
                    ColumnInfo _ci = _tableMeta.getFieldMap().get(key);
                    Attr _attr = _ci.toAttr();
                    if (__readonlyFields.contains(_attr.getColumnName().toLowerCase())) {
                        _attr.setReadonly(true);
                    }
                    _fieldList.add(_attr);
                    _fieldListForNotNullable.add(_attr);
                    _allFieldList.add(new Attr("String",
                            __doNamedFilter(_ci.getColumnName()).toUpperCase(),
                            _ci.getColumnName(),
                            _ci.isAutoIncrement(),
                            _ci.isSigned(),
                            _ci.getPrecision(),
                            _ci.getScale(),
                            _ci.getNullable(),
                            _ci.getDefaultValue(),
                            _ci.getRemarks()));
                }
            } else {
                if (!view) {
                    _propMap.put("primaryKeyType", _tableMeta.getFieldMap().get(_tableMeta.getPkSet().get(0)).getColumnType());
                    _propMap.put("primaryKeyName", StringUtils.uncapitalize(EntityMeta.propertyNameToFieldName(_tableMeta.getPkSet().get(0))));
                } else {
                    ColumnInfo _tmpCI = _tableMeta.getFieldMap().get("id");
                    _propMap.put("primaryKeyType", _tmpCI == null ? "Serializable" : _tmpCI.getColumnType());
                    _propMap.put("primaryKeyName", "id");
                }
                for (String key : _tableMeta.getFieldMap().keySet()) {
                    ColumnInfo _ci = _tableMeta.getFieldMap().get(key);
                    Attr _attr = _ci.toAttr();
                    if (__readonlyFields.contains(_attr.getColumnName().toLowerCase())) {
                        _attr.setReadonly(true);
                    }
                    _fieldList.add(_attr);
                    if (_attr.getNullable() == 0) {
                        _fieldListForNotNullable.add(_attr);
                    }
                    _allFieldList.add(new Attr("String",
                            __doNamedFilter(_ci.getColumnName()).toUpperCase(),
                            _ci.getColumnName(),
                            _ci.isAutoIncrement(),
                            _ci.isSigned(),
                            _ci.getPrecision(),
                            _ci.getScale(),
                            _ci.getNullable(),
                            _ci.getDefaultValue(),
                            _ci.getRemarks()));
                }
            }
            _propMap.put("fieldList", _fieldList);
            // 为必免构造方法重复，构造参数数量相同则清空
            _propMap.put("notNullableFieldList", _fieldList.size() == _fieldListForNotNullable.size() ? Collections.emptyList() : _fieldListForNotNullable);
            _propMap.put("allFieldList", _allFieldList);
            //
            buildTargetFile("/model/" + _modelName + (((Boolean) _propMap.get("isUseClassSuffix")) ? "Model.java" : ".java"), view ? "/View.ftl" : "/Entity.ftl", _propMap);
            //
            if (!view) {
                if (_tableMeta.getPkSet().size() > 1) {
                    _propMap.put("modelName", _modelName);
                    if (_tableMeta.getPkSet().size() > 1) {
                        List<Attr> _primaryKeyList = new ArrayList<Attr>();
                        _propMap.put("primaryKeyList", _primaryKeyList);
                        //
                        for (String pkey : _tableMeta.getPkSet()) {
                            ColumnInfo _ci = _tableMeta.getFieldMap().get(pkey);
                            _primaryKeyList.add(_ci.toAttr());
                        }
                    }
                    buildTargetFile("/model/" + _modelName + "PK.java", "/EntityPK.ftl", _propMap);
                }
            }
        }
    }

    private void buildTargetFile(String targetFileName, String tmplFile, Map<String, Object> propMap) {
        Writer _outWriter = null;
        try {
            File _outputFile = new File(RuntimeUtils.replaceEnvVariable(StringUtils.defaultIfBlank(__owner.getConfig().getParam("jdbc.output_path"), "${root}")), new File(((String) propMap.get("packageName")).replace('.', '/'), targetFileName).getPath());
            File _path = _outputFile.getParentFile();
            if (!_path.exists()) {
                _path.mkdirs();
            }
            Template _template = __freemarkerConfig.getTemplate(__templateRootPath + tmplFile);
            _outWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(_outputFile), StringUtils.defaultIfEmpty(__freemarkerConfig.getOutputEncoding(), __freemarkerConfig.getDefaultEncoding())));
            _template.process(propMap, _outWriter);
            System.out.println("Output file \"" + _outputFile + "\".");
        } catch (Exception e) {
            _LOG.warn("", e);
        } finally {
            if (_outWriter != null) {
                try {
                    _outWriter.flush();
                    _outWriter.close();
                } catch (IOException e) {
                    _LOG.warn("", e);
                }
            }
        }
    }

    private Map<String, Object> buildPropMap() {
        Map<String, Object> _propMap = new HashMap<String, Object>();
        _propMap.put("packageName", StringUtils.defaultIfBlank(__owner.getConfig().getParam("jdbc.package_name"), "packages"));
        _propMap.put("lastUpdateTime", new Date());
        //
        //
        boolean _isUseBaseEntity = BlurObject.bind(__owner.getConfig().getParam("jdbc.use_base_entity")).toBooleanValue();
        boolean _isUseClassSuffix = BlurObject.bind(__owner.getConfig().getParam("jdbc.use_class_suffix")).toBooleanValue();
        boolean _isUseChainMode = BlurObject.bind(__owner.getConfig().getParam("jdbc.use_chain_mode")).toBooleanValue();
        boolean _isUseStateSupport = BlurObject.bind(__owner.getConfig().getParam("jdbc.use_state_support")).toBooleanValue();
        _propMap.put("isUseBaseEntity", _isUseBaseEntity);
        _propMap.put("isUseClassSuffix", _isUseClassSuffix);
        _propMap.put("isUseChainMode", _isUseChainMode);
        _propMap.put("isUseStateSupport", _isUseStateSupport);
        if (_isUseBaseEntity) {
            buildTargetFile("/model/BaseEntity.java", "/BaseEntity.ftl", _propMap);
        }
        //
        return _propMap;
    }

    private static class TableMeta {
        private List<String> pkSet;
        private Map<String, ColumnInfo> fieldMap;

        public TableMeta(List<String> pkSet, Map<String, ColumnInfo> fieldMap) {
            this.pkSet = pkSet;
            this.fieldMap = fieldMap;
        }

        public List<String> getPkSet() {
            return pkSet;
        }

        public Map<String, ColumnInfo> getFieldMap() {
            return fieldMap;
        }
    }

    private static class ColumnInfo {

        private String varName;
        //
        private String columnName;
        private String columnType;
        private boolean autoIncrement;
        private boolean isSigned;
        private int precision;
        private int scale;
        private int nullable;
        private String defaultValue;
        private String remarks;

        public ColumnInfo(IEntityNamedFilter namedFilter, String columnName, String columnType, boolean autoIncrement, boolean isSigned, int precision, int scale, int nullable, String defaultValue, String remarks) {
            this.columnName = columnName;
            this.columnType = columnType;
            this.autoIncrement = autoIncrement;
            this.isSigned = isSigned;
            this.precision = precision;
            this.scale = scale;
            this.nullable = nullable;
            this.defaultValue = defaultValue;
            this.remarks = remarks;
            //
            if (namedFilter != null) {
                this.varName = StringUtils.defaultIfBlank(namedFilter.doFilter(columnName), columnName);
            } else {
                this.varName = columnName;
            }
            this.varName = StringUtils.uncapitalize(EntityMeta.propertyNameToFieldName(this.varName.toLowerCase()));
        }

        public String getColumnName() {
            return columnName;
        }

        public String getColumnType() {
            return columnType;
        }

        public boolean isAutoIncrement() {
            return autoIncrement;
        }

        public boolean isSigned() {
            return isSigned;
        }

        public int getPrecision() {
            return precision;
        }

        public int getScale() {
            return scale;
        }

        public int getNullable() {
            return nullable;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public String getRemarks() {
            return remarks;
        }

        public Attr toAttr() {
            return new Attr(getColumnType(), this.varName, getColumnName(), isAutoIncrement(), isSigned(), getPrecision(), getScale(), getNullable(), getDefaultValue(), getRemarks());
        }
    }

    public static class Attr {
        String varType;
        String varName;
        String columnName;
        boolean autoIncrement;
        private boolean isSigned;
        private int precision;
        private int scale;
        int nullable;
        String defaultValue;
        String remarks;
        boolean readonly;

        public Attr(String varType, String varName, String columnName, boolean autoIncrement, boolean isSigned, int precision, int scale, int nullable, String defaultValue, String remarks) {
            this.varName = varName;
            this.varType = varType;
            this.columnName = columnName;
            this.autoIncrement = autoIncrement;
            this.isSigned = isSigned;
            try {
                if (!isSigned && !ClassUtils.isSubclassOf(Class.forName(varType), Number.class)) {
                    this.isSigned = true;
                }
            } catch (Exception ignored) {
            }
            this.precision = precision;
            this.scale = scale;
            this.nullable = nullable;
            this.defaultValue = defaultValue;
            this.remarks = remarks;
        }

        public String getVarType() {
            return varType;
        }

        public String getVarName() {
            return varName;
        }

        public String getColumnName() {
            return columnName;
        }

        public boolean isAutoIncrement() {
            return autoIncrement;
        }

        public boolean isSigned() {
            return isSigned;
        }

        public int getPrecision() {
            return precision;
        }

        public int getScale() {
            return scale;
        }

        public int getNullable() {
            return nullable;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public String getRemarks() {
            return remarks;
        }

        public void setReadonly(boolean readonly) {
            this.readonly = readonly;
        }

        public boolean isReadonly() {
            return readonly;
        }

        @Override
        public String toString() {
            return this.getVarName();
        }

    }

    public static void main(String[] args) throws Exception {
        YMP.get().init();
        try {
            new EntityGenerator(YMP.get()).createEntityClassFiles((args != null && args.length > 0) && BlurObject.bind(args[0]).toBooleanValue());
        } finally {
            YMP.get().destroy();
        }
    }
}
