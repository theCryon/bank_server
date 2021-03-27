import java.math.BigDecimal;
import java.sql.*;
import java.util.Random;

public class Backend {
    private enum State {WAITING, MENU, ACCOUNT_MENU, LOGIN, DEPOSIT, TRANSFER}
    private State state = State.WAITING;
    private Account accProcess;

    public String processInput(String theInput, Connection con) {
        createTable(con);
        String theOutput = null;
        if (state == State.WAITING) {
            theOutput = "1. Create an account " +
                        "2. Log into account " +
                        "0. Exit " +
                        "Press any key to print MENU. ";
            state = State.MENU;
        } else if (state == State.MENU) {
            switch (theInput) {
                case "0":
                    theOutput = "Bye.";
                    state = State.WAITING;
                    break;
                case "1":
                    Account acc = createAccount();
                    updateAccountToDatabase(con, acc);
                    theOutput = printAccountCreated(acc.getCardNumber(), acc.getCardPIN());
                    state = State.MENU;
                    break;
                case "2":
                    theOutput = "Proceeding to login panel... " +
                                "Please enter your card number and PIN " +
                                "In format [card number][SPACE][PIN]";
                    state = State.LOGIN;
                    break;
                default:
                    theOutput = "1. Create an account " +
                                "2. Log into account " +
                                "0. Exit " +
                                "Press any key to print MENU. ";
                    state = State.MENU;
            }
        }
        else if (state == State.LOGIN) {
            if ((accProcess = loginIntoAccount(con, theInput)) != null){
                theOutput = "Successfully logged in " +
                            "1. Balance " +
                            "2. Add income " +
                            "3. Do transfer " +
                            "0. Exit";
                state = State.ACCOUNT_MENU;
            } else {
                theOutput = "Error logging in ";
                state = State.WAITING;
            }
        } else if (state == State.ACCOUNT_MENU) {
            switch (theInput) {
                case "1":
                    accProcess.setBalance(extractBalanceFromDataBase(con, accProcess.getCardNumber()));
                    theOutput = "Balance: " + accProcess.getBalance();
                    state = State.ACCOUNT_MENU;
                    break;
                case "2":
                    theOutput = "How much do you want to deposit?: ";
                    state = State.DEPOSIT;
                    break;
                case "3":
                    theOutput = "Enter target account number and the amount you want to transfer:";
                    state = State.TRANSFER;
                    break;
                case "0":
                    theOutput = "Bye.";
                    state = State.WAITING;
                    break;
                default:
                    theOutput = "1. Balance " +
                                "2. Add income " +
                                "3. Do transfer " +
                                "0. Exit";
                    state = State.WAITING;
                    break;
            }
        } else if (state == State.DEPOSIT) {
            theOutput = addIncome(con, accProcess, BigDecimal.valueOf(Integer.parseInt(theInput)));
            state = State.ACCOUNT_MENU;
        } else if (state == State.TRANSFER) {
            theOutput = transfer(con, accProcess, theInput);
            state = State.ACCOUNT_MENU;
        }
        return theOutput;
    }

    private String generateCardPIN() {
        Random random = new Random();
        StringBuilder stringBuilder = new StringBuilder(4);
        for (int i = 0; i < stringBuilder.capacity(); i++) {
            stringBuilder.append(random.nextInt(9));
        }
        return stringBuilder.toString();
    }

    private String generateCardNumber() {
        Random random = new Random();
        StringBuilder stringBuilder = new StringBuilder();
        String BIN = "400000";
        stringBuilder.append(BIN);
        for (int i = 0; i < 9; i++) {
            stringBuilder.append(random.nextInt(9));
        }
        int controlNumber = 0;
        int tmp;
        for (int i = 0; i < 15; i++) {
            tmp = (i % 2 != 0)
                    ? Integer.parseInt(String.valueOf(stringBuilder.charAt(i)))
                    : Integer.parseInt(String.valueOf(stringBuilder.charAt(i))) * 2;
            tmp = tmp > 9 ? tmp - 9 : tmp;
            controlNumber += tmp;
        }
        int checksum = 0;
        if (controlNumber % 10 != 0) {
            while ((controlNumber + checksum) % 10 != 0) {
                checksum++;
            }
        }
        stringBuilder.append(checksum);
        return stringBuilder.toString();
    }

    private boolean checkForLuhn(String cardNumber) {
        StringBuilder sb = new StringBuilder();
        sb.append(cardNumber);
        int tmp;
        int sum = 0;
        for (int i = 0; i < 15; i++) {
            tmp = (i % 2 != 0)
                    ? Integer.parseInt(String.valueOf(sb.charAt(i)))
                    : Integer.parseInt(String.valueOf(sb.charAt(i))) * 2;
            tmp = tmp > 9 ? tmp - 9 : tmp;
            sum += tmp;
        }
        int checksum = Integer.parseInt(String.valueOf(sb.charAt(15)));
        return (sum + checksum) % 10 == 0;
    }

    private Account createAccount() {
        System.out.println("New account has been created!");
        Account acc = new Account();
        acc.setCardNumber(generateCardNumber());
        acc.setCardPIN(generateCardPIN());
        acc.setBalance(BigDecimal.ZERO);
        return acc;
    }

    private String printAccountCreated(String cardNumber, String cardPIN) {
        return "Your account has been created " +
                "Your card number: " + cardNumber +
                " Your card PIN: " + cardPIN;
    }

    private Account loginIntoAccount(Connection con, String input) {
        String[] str = input.split("\\s");
        String cardNumber = str[0];
        String cardPIN = str[1];
        Account acc;
        if (checkValidLogin(con, cardNumber, cardPIN)) {
            acc = new Account(cardNumber, cardPIN, BigDecimal.ZERO);
        } else {
            acc = null;
        }
        return acc;
    }

    private static void createTable(Connection con) {
        try (Statement statement = con.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS card (" +
                    "id INTEGER PRIMARY KEY," +
                    "number TEXT," +
                    "pin TEXT," +
                    "balance DECIMAL(12,2) DEFAULT 0 " +
                    ");");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateAccountToDatabase(Connection con, Account account) {
        String sql = "INSERT INTO card(number, pin) VALUES (?,?)";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, account.getCardNumber());
            pstmt.setString(2, account.getCardPIN());
            pstmt.executeUpdate();
            con.commit();
        } catch (SQLException e) {
            try {
                System.err.println("Couldn't update the account to the database. Rolling back changes.");
                con.rollback();
            } catch (SQLException rollbackE) {
                rollbackE.printStackTrace();
            }
        }
    }

    private String extractFromDataBase(Connection con, String column, String value) {
        String sql = "SELECT " + column + " FROM card WHERE " + column + " == ?;";
        String result = null;
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, value);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                result = rs.getString(column);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    private BigDecimal extractBalanceFromDataBase(Connection con, String number) {
        String sql = "SELECT balance FROM card WHERE number == ?;";
        double result = 0.0;
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, number);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                result = rs.getDouble("balance");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return BigDecimal.valueOf(result);
    }

    private boolean checkValidLogin(Connection con, String accountNumber, String accountPIN) {
        String[] results = new String[2];
        results[0] = extractFromDataBase(con, "number", accountNumber);
        results[1] = extractFromDataBase(con, "pin", accountPIN);
        return accountNumber.equals(results[0]) && accountPIN.equals(results[1]);
    }

    private String addIncome(Connection con, Account acc, BigDecimal balance) {
        String result;
        String sql = "UPDATE card SET balance = balance + ? WHERE number == ? AND pin == ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setBigDecimal(1, balance);
            pstmt.setString(2, acc.getCardNumber());
            pstmt.setString(3, acc.getCardPIN());
            pstmt.executeUpdate();
            con.commit();
            acc.setBalance(balance);
            result = "Income added.";
        } catch (SQLException e) {
            try {
                result = "Couldn't add income. Rolling back changes";
                con.rollback();
            } catch (SQLException rollbackE) {
                rollbackE.printStackTrace();
                result = "SQL Error.";
            }
        }
        return result;
    }

    private String transfer(Connection con, Account acc, String str) {
        String result;
        String[] splitStr = str.split("\\s");
        String targetAccount = splitStr[0];
        if (targetAccount.equals(extractFromDataBase(con, "number", targetAccount))) {
            double transferAmount = Double.parseDouble(splitStr[1]);
            int res = extractBalanceFromDataBase(con, acc.getCardNumber()).compareTo(BigDecimal.valueOf(transferAmount));
            if (res >= 0) {
                String sqlGiver = "UPDATE card SET balance = balance - ? WHERE number == ?;";
                String sqlRecipient = "UPDATE card SET balance = balance + ? WHERE number == ?;";
                try (PreparedStatement pstmtG = con.prepareStatement(sqlGiver);
                     PreparedStatement pstmtR = con.prepareStatement(sqlRecipient)) {
                    pstmtG.setInt(1, (int) transferAmount);
                    pstmtG.setString(2, acc.getCardNumber());
                    pstmtG.executeUpdate();

                    pstmtR.setInt(1, (int) transferAmount);
                    pstmtR.setString(2, targetAccount);
                    pstmtR.executeUpdate();
                    con.commit();
                    result = "Money transferred.";
                } catch (SQLException e) {
                    try {
                        result = "Couldn't transfer money. Rolling back changes.";
                        con.rollback();
                    } catch (SQLException rollbackE) {
                        rollbackE.printStackTrace();
                        result = "SQL ERROR";
                    }
                }
            } else {
                result = "Not enough money! Transfer canceled.";
            }
        } else if(!checkForLuhn(targetAccount)) {
            result = "Probably you have made a mistake in the card number. Please try again.";
        } else {
            result = "Such a card does not exist.";
        }
        return result;
    }
}

