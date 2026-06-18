package edu.course.brigdelt.repository;

import edu.course.brigdelt.domain.Country;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Persists country configuration into SQLite.
 */
public class CountryRepository {

    private final DatabaseManager databaseManager;

    public CountryRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void upsertAll(List<Country> countries) {
        String sql = """
                INSERT INTO countries (
                    cameo_code,
                    iso_code,
                    name_cn,
                    name_en,
                    region,
                    latitude,
                    longitude,
                    is_bri_country,
                    is_core_country
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(cameo_code) DO UPDATE SET
                    iso_code = excluded.iso_code,
                    name_cn = excluded.name_cn,
                    name_en = excluded.name_en,
                    region = excluded.region,
                    latitude = excluded.latitude,
                    longitude = excluded.longitude,
                    is_bri_country = excluded.is_bri_country,
                    is_core_country = excluded.is_core_country
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            connection.setAutoCommit(false);
            for (Country country : countries) {
                statement.setString(1, country.cameoCode());
                statement.setString(2, country.isoCode());
                statement.setString(3, country.nameCn());
                statement.setString(4, country.nameEn());
                statement.setString(5, country.region());
                setNullableDouble(statement, 6, country.latitude());
                setNullableDouble(statement, 7, country.longitude());
                statement.setInt(8, country.briCountry() ? 1 : 0);
                statement.setInt(9, country.coreCountry() ? 1 : 0);
                statement.addBatch();
            }
            statement.executeBatch();
            connection.commit();
        } catch (SQLException exception) {
            throw new IllegalStateException("国家配置入库失败。", exception);
        }
    }

    public int countCountries() {
        String sql = "SELECT COUNT(*) FROM countries";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("国家数量统计失败。", exception);
        }
    }

    public boolean existsByCameoCode(String cameoCode) {
        String sql = "SELECT 1 FROM countries WHERE cameo_code = ? LIMIT 1";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, cameoCode);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("国家查询失败：" + cameoCode, exception);
        }
    }

    public Set<String> findAllCameoCodes() {
        String sql = "SELECT cameo_code FROM countries WHERE cameo_code IS NOT NULL AND TRIM(cameo_code) <> ''";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            Set<String> cameoCodes = new HashSet<>();
            while (resultSet.next()) {
                cameoCodes.add(resultSet.getString("cameo_code").trim().toUpperCase(Locale.ROOT));
            }
            return cameoCodes;
        } catch (SQLException exception) {
            throw new IllegalStateException("国家 CAMEO code 查询失败。", exception);
        }
    }

    public List<Country> findAllCountries() {
        String sql = """
                SELECT cameo_code, iso_code, name_cn, name_en, region, latitude, longitude,
                       is_bri_country, is_core_country
                FROM countries
                WHERE cameo_code IS NOT NULL AND TRIM(cameo_code) <> ''
                ORDER BY is_core_country DESC, region, cameo_code
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<Country> countries = new ArrayList<>();
            while (resultSet.next()) {
                countries.add(new Country(
                        resultSet.getString("cameo_code"),
                        resultSet.getString("iso_code"),
                        resultSet.getString("name_cn"),
                        resultSet.getString("name_en"),
                        resultSet.getString("region"),
                        nullableDouble(resultSet, "latitude"),
                        nullableDouble(resultSet, "longitude"),
                        resultSet.getInt("is_bri_country") == 1,
                        resultSet.getInt("is_core_country") == 1
                ));
            }
            return countries;
        } catch (SQLException exception) {
            throw new IllegalStateException("国家列表查询失败。", exception);
        }
    }

    private Double nullableDouble(ResultSet resultSet, String columnName) throws SQLException {
        double value = resultSet.getDouble(columnName);
        return resultSet.wasNull() ? null : value;
    }

    private void setNullableDouble(PreparedStatement statement, int parameterIndex, Double value)
            throws SQLException {
        if (value == null) {
            statement.setNull(parameterIndex, Types.REAL);
            return;
        }
        statement.setDouble(parameterIndex, value);
    }
}
