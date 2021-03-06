package halo.dal.partition.analysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

public class DefSQLAnalyzer implements SQLAnalyzer {

    private final static String SQL_KEY_LEFT_PARENTHESIS = "(";

    private final static String SQL_KEY_RIGHT_PARENTHESIS = ")";

    private final static Set<String> keySet = new HashSet<String>();

    private final static String SQL_BLANK = " ";

    private final static String SQL_KEY_SELECT = "select ";

    private final static String SQL_KEY_LEFT_JOIN = "left join ";

    private final static String SQL_KEY_RIGHT_JOIN = "rihgt join ";

    private final static String SQL_KEY_FULL_JOIN = "full join ";

    private final static String SQL_KEY_INNER_JOIN = "inner join ";

    private final static String SQL_KEY_ON = "on ";

    private final static String SQL_KEY_UPDATE = "update ";

    private final static String SQL_KEY_SET = "set ";

    private final static String SQL_KEY_WHERE = "where ";

    private final static String SQL_KEY_FROM = "from ";

    private final static String SQL_KEY_INSERT_INTO = "insert into ";

    private final static String SQL_KEY_DELETE_FROM = "delete from ";

    private final static String KEY_SELECT = "select";

    private final static String KEY_INSERT = "insert";

    private final static String KEY_UPDATE = "update";

    private final static String KEY_DELETE = "delete";

    private final static String KEY_INTO = "into";

    private final static String KEY_FROM = "from";

    private final static String KEY_FULL = "full";

    private final static String KEY_JOIN = "join";

    private final static String KEY_LEFT = "left";

    private final static String KEY_RIGHT = "right";

    private final static String KEY_INNER = "inner";

    private final static String KEY_VALUES = "values";

    private final static String KEY_SET = "set";

    private final static String KEY_ON = "on";

    private final static String KEY_WHERE = "where";
    static {
        keySet.add(KEY_SELECT);
        keySet.add(KEY_FROM);
        keySet.add(KEY_FULL);
        keySet.add(KEY_INNER);
        keySet.add(KEY_LEFT);
        keySet.add(KEY_RIGHT);
        keySet.add(KEY_JOIN);
        keySet.add(KEY_ON);
        keySet.add(KEY_WHERE);
        keySet.add(KEY_DELETE);
        keySet.add(KEY_INSERT);
        keySet.add(KEY_UPDATE);
        keySet.add(KEY_INTO);
        keySet.add(KEY_VALUES);
        keySet.add(KEY_SET);
    }

    public String outPutSQL(SQLInfo sqlInfo) {
        BasicSQLInfo info = (BasicSQLInfo) sqlInfo;
        String sql = info.getOriginalSQL();
        List<String> list = new ArrayList<String>();
        List<String> newList = new ArrayList<String>();
        for (String tableName : info.getTableNames()) {
            String alias = info.getAliasByTableName(tableName);
            boolean isSame = alias != null && alias.endsWith(tableName);
            if (!isSame) {
                list.add(" " + tableName + ".");
                newList.add(" " + info.getRealTableName(tableName) + ".");
            }
            list.add(" " + tableName + " ");
            newList.add(" " + info.getRealTableName(tableName) + " ");
            list.add(" " + tableName + "(");
            newList.add(" " + info.getRealTableName(tableName) + "(");
            list.add("," + tableName + " ");
            newList.add("," + info.getRealTableName(tableName) + " ");
        }
        return StringUtils.replaceEach(sql,
                list.toArray(new String[list.size()]),
                newList.toArray(new String[newList.size()]));
    }

    public SQLInfo analyse(String sql, Object[] values) {
        if (sql.indexOf(" between ") != -1) {
            throw new SQLKeyErrException("not supported sql key: between ");
        }
        String _sql = sql.replaceAll("\\. {1,}", "\\.").trim();
        BasicSQLInfo sqlInfo = new BasicSQLInfo(_sql, values);
        this.parseSQLSegment(sqlInfo, _sql);
        if (_sql.indexOf(SQL_KEY_SELECT) != -1) {
            this.parseSelectTable(sqlInfo);
        }
        else if (_sql.indexOf(SQL_KEY_INSERT_INTO) != -1) {
            this.parseInsertTable(sqlInfo);
        }
        else if (_sql.indexOf(SQL_KEY_UPDATE) != -1) {
            this.parseUpdateTable(sqlInfo);
        }
        else if (_sql.indexOf(SQL_KEY_DELETE_FROM) != -1) {
            this.parseDeleteTable(sqlInfo);
        }
        else {
            throw new SQLAnalyzerException(
                    "sql key must lower case. or sql is not supported :" + _sql);
        }
        this.parseSQLExpressions(sqlInfo, values);
        return sqlInfo;
    }

    private void parseSelectTable(BasicSQLInfo sqlInfo) {
        String tableNameSeg;
        for (String fromSQL : sqlInfo.getFromSQLs()) {
            tableNameSeg = fromSQL.substring(SQL_KEY_FROM.length());
            String[] tables = tableNameSeg.split(",");
            for (String table : tables) {
                sqlInfo.addTable(table.trim());
            }
            tableNameSeg = sqlInfo.getJoinSQL();
            int len = 0;
            if (tableNameSeg != null) {
                int idx = tableNameSeg.indexOf(SQL_KEY_FULL_JOIN);
                len = SQL_KEY_FULL_JOIN.length();
                if (idx == -1) {
                    idx = tableNameSeg.indexOf(SQL_KEY_INNER_JOIN);
                    len = SQL_KEY_INNER_JOIN.length();
                }
                if (idx == -1) {
                    idx = tableNameSeg.indexOf(SQL_KEY_LEFT_JOIN);
                    len = SQL_KEY_LEFT_JOIN.length();
                }
                if (idx == -1) {
                    idx = tableNameSeg.indexOf(SQL_KEY_RIGHT_JOIN);
                    len = SQL_KEY_RIGHT_JOIN.length();
                }
                String table = tableNameSeg.substring(idx + len).trim();
                sqlInfo.addTable(table);
            }
        }
    }

    private void parseInsertTable(BasicSQLInfo sqlInfo) {
        String insertIntoSeg = sqlInfo.getInsertIntoSQL();
        String table = insertIntoSeg.substring(SQL_KEY_INSERT_INTO.length(),
                insertIntoSeg.indexOf(SQL_KEY_LEFT_PARENTHESIS)).trim();
        sqlInfo.addTable(table);
    }

    private void parseDeleteTable(BasicSQLInfo sqlInfo) {
        String table = sqlInfo.getDeleteFromSQL().substring(
                SQL_KEY_DELETE_FROM.length());
        sqlInfo.addTable(table);
    }

    private void parseUpdateTable(BasicSQLInfo sqlInfo) {
        String table = sqlInfo.getUpdateSQL()
                .substring(SQL_KEY_UPDATE.length());
        sqlInfo.addTable(table);
    }

    private String parseLogicTableName(BasicSQLInfo sqlInfo, String columnSeg) {
        int idx = columnSeg.indexOf(".");
        if (idx == -1) {
            return null;
        }
        String alias = columnSeg.substring(0, idx);
        String logicTableName = sqlInfo.getTableNameByAalias(alias);
        if (logicTableName == null) {
            return alias;
        }
        return logicTableName;
    }

    private void parseSQLExpressions(BasicSQLInfo sqlInfo, Object[] values) {
        // insert sql
        if (sqlInfo.getInsertIntoSQL() != null) {
            String insertSeg = sqlInfo.getInsertIntoSQL();
            int idxLeftParenthesis = insertSeg
                    .indexOf(SQL_KEY_LEFT_PARENTHESIS);
            int idxRightParenthesis = insertSeg
                    .indexOf(SQL_KEY_RIGHT_PARENTHESIS);
            String columns = insertSeg.substring(idxLeftParenthesis + 1,
                    idxRightParenthesis);
            String[] columnarr = columns.split(",");
            SQLExpression sqlExpression;
            String field;
            String logicTableName;
            for (int i = 0; i < columnarr.length; i++) {
                field = columnarr[i].trim();
                logicTableName = this.parseLogicTableName(sqlInfo, field);
                sqlExpression = new SQLExpression();
                sqlExpression.setColumn(field);
                sqlExpression.setValue(values[i]);
                sqlExpression.setSqlExpressionSymbol(SQLExpressionSymbol.EQUAL);
                sqlInfo.addSQLExpression(logicTableName, sqlExpression);
            }
        }
        else {
            int beginIdx = 0;
            // update sql
            if (sqlInfo.getUpdateSetSQL() != null) {
                String exSeg = sqlInfo.getUpdateSetSQL().substring(
                        SQL_KEY_SET.length());
                String[] expressionSegs = exSeg.split(",");
                SQLExpression sqlExpression;
                String logicTableName;
                String seg;
                for (int i = 0; i < expressionSegs.length; i++) {
                    beginIdx++;
                    seg = expressionSegs[i].trim();
                    logicTableName = this.parseLogicTableName(sqlInfo, seg);
                    sqlExpression = new SQLExpression(seg, values[i]);
                    sqlInfo.addSQLExpression(logicTableName, sqlExpression);
                }
            }
            // where sql
            if (sqlInfo.getWhereSQL() != null) {
                String whereSQL = sqlInfo.getWhereSQL().replaceAll("\\(|\\)",
                        "");
                String seg = whereSQL.substring(SQL_KEY_WHERE.length()).trim();
                String[] kv = seg.split("and|or");
                SQLExpression sqlExpression;
                int valueIdx = beginIdx;
                String logicTableName;
                String kvSeg;
                for (int i = 0; i < kv.length; i++) {
                    if (kv[i].indexOf("?") == -1) {
                        continue;
                    }
                    if (!SQLExpression.isKeyValue(kv[i])) {
                        continue;
                    }
                    kvSeg = kv[i].trim();
                    logicTableName = this.parseLogicTableName(sqlInfo, kvSeg);
                    sqlExpression = new SQLExpression(kvSeg, values[valueIdx]);
                    sqlInfo.addSQLExpression(logicTableName, sqlExpression);
                    valueIdx++;
                }
            }
        }
    }

    private void parseSQLSegment(BasicSQLInfo sqlInfo, String sql) {
        List<String> list = formatSQL(sql);
        for (String s : list) {
            if (s.startsWith(SQL_KEY_SELECT)) {
                sqlInfo.setSelectSQL(s);
            }
            else if (s.startsWith(SQL_KEY_LEFT_JOIN) || //
                    s.startsWith(SQL_KEY_RIGHT_JOIN) || //
                    s.startsWith(SQL_KEY_INNER_JOIN) || //
                    s.startsWith(SQL_KEY_FULL_JOIN) //
            ) {
                sqlInfo.setJoinSQL(s);
            }
            else if (s.startsWith(SQL_KEY_ON)) {
                sqlInfo.setJoinOnSQL(s);
            }
            else if (s.startsWith(SQL_KEY_WHERE)) {
                sqlInfo.setWhereSQL(s);
            }
            else if (s.startsWith(SQL_KEY_FROM)) {
                sqlInfo.addFromSQL(s);
            }
            else if (s.startsWith(SQL_KEY_DELETE_FROM)) {
                sqlInfo.setDeleteFromSQL(s);
            }
            else if (s.startsWith(SQL_KEY_UPDATE)) {
                sqlInfo.setUpdateSQL(s);
            }
            else if (s.startsWith(SQL_KEY_SET)) {
                sqlInfo.setUpdateSetSQL(s);
            }
            else if (s.startsWith(SQL_KEY_INSERT_INTO)) {
                sqlInfo.setInsertIntoSQL(s);
            }
        }
    }

    /**
     * 格式化sql，输出为一个sql片段集合
     * 
     * @param sql
     * @return
     */
    private List<String> formatSQL(String sql) {
        String[] sqlWords = sql.split(" ");
        List<String> wordList = new ArrayList<String>();
        for (String word : sqlWords) {
            if (word.equals(SQL_BLANK)) {
                continue;
            }
            wordList.add(word);
        }
        boolean onKey = false;
        List<String> list = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        String word;
        for (int i = 0; i < wordList.size(); i++) {
            word = wordList.get(i);
            if (onKey) {
                onKey = hasKey(word);
                sb.append(word).append(SQL_BLANK);
            }
            else {
                if (hasKey(word)) {
                    onKey = true;
                    if (sb.length() > 0) {
                        list.add(sb.toString().trim());
                    }
                    sb.delete(0, sb.length());
                    sb.append(word).append(SQL_BLANK);
                }
                else {
                    sb.append(word).append(SQL_BLANK);
                    onKey = false;
                }
            }
        }
        if (sb.length() > 0) {
            list.add(sb.toString().trim());
        }
        return list;
    }

    private boolean hasKey(String key) {
        return keySet.contains(key);
    }
}