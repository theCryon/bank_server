import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.Socket;
import java.sql.*;
import java.util.Random;
import java.util.Scanner;

class Backend {
    private final Scanner scanner = new Scanner(System.in);
    private final Socket socket;
    private final Connection con;

    Backend(Socket socket, Connection con) {
        this.socket = socket;
        this.con = con;
    }

    private String readFromClient() {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String str = null;
        try {
            str = br.readLine();
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return str;
    }

    private void writeToClient(String str) {
        PrintWriter printWriter;
        try {
            printWriter = new PrintWriter(socket.getOutputStream());
            printWriter.println(str);
            printWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void menu() {
        createTable(con);
        while (true) {
            writeToClient(printMenu());
            switch (readFromClient()) {
                case "1":
//                    Account acc = createAccount();
//                    updateAccountToDatabase(con, acc);
//                    printAccountCreated(acc.getCardNumber(), acc.getCardPIN());
                    System.out.println("1");
                    break;
                case "2":
                    //loginIntoAccount(con);
                    System.out.println("2");
                    break;
                case "0":
                    System.out.println("Bye!");
                    return;
                default:
                    break;
            }
        }
    }

    private String printMenu() {
        return "1. Create an account\n" + "2. Log into account\n" + "0. Exit\n";
    }

    private String printAccountMenu() {
        return "1. Balance\n" + "2. Add income\n" + "3. Do transfer\n" + "4. Close account\n" + "5. Log out\n" + "0. Exit\n";
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
        Account acc = new Account();
        acc.setCardNumber(generateCardNumber());
        acc.setCardPIN(generateCardPIN());
        acc.setBalance(BigDecimal.ZERO);
        return acc;
    }

    private void printAccountCreated(String cardNumber, String cardPIN) {
        System.out.println("Your card has been created");
        System.out.println("Your card number:");
        System.out.println(cardNumber);
        System.out.println("Your card PIN:");
        System.out.println(cardPIN);
    }

    private void loginIntoAccount(Connection con) {
        System.out.println("Enter your card number:");
        String cardNumber = scanner.next();
        System.out.println("Enter your PIN:");
        String cardPIN = scanner.next();
        if (checkValidLogin(con, cardNumber, cardPIN)) {
            Account newAcc = new Account(cardNumber, cardPIN, BigDecimal.ZERO);
            accountMenu(con, newAcc);
        } else {
            System.out.println("Wrong card number or PIN!");
        }
    }

    private void accountMenu(Connection con, Account acc) {
        System.out.println("You have successfully logged in!");
        while (true) {
            printAccountMenu();
            switch (scanner.next()) {
                case "1":
                    acc.setBalance(extractBalanceFromDataBase(con, acc.getCardNumber()));
                    System.out.println("Balance: " + acc.getBalance());
                    break;
                case "2":
                    System.out.println("Enter income: ");
                    addIncome(con, acc, scanner.nextBigDecimal());
                    System.out.println("Income was added!");
                    break;
                case "3":
                    transfer(con, acc);
                    break;
                case "4":
                    closeAccount(con, acc);
                    return;
                case "5":
                    return;
                case "0":
                    System.out.println("Bye!");
                    System.exit(0);
            }
        }
    }

    private void createTable(Connection con) {
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

//    private BigDecimal getBalanceFromDataBase(Connection con, Account acc) {
//        BigDecimal balance = BigDecimal.ZERO;
//        String sql = "SELECT balance FROM card WHERE number == ? AND pin == ?";
//        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
//            pstmt.setString(1, acc.getCardNumber());
//            pstmt.setString(2, acc.getCardPIN());
//            ResultSet rs = pstmt.executeQuery();
//            while (rs.next()) {
//                balance = rs.getBigDecimal("balance");
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//        return balance;
//    }

    private boolean checkValidLogin(Connection con, String accountNumber, String accountPIN) {
        String[] results = new String[2];
        results[0] = extractFromDataBase(con, "number", accountNumber);
        results[1] = extractFromDataBase(con, "pin", accountPIN);
        return accountNumber.equals(results[0]) && accountPIN.equals(results[1]);
    }

    private void addIncome(Connection con, Account acc, BigDecimal balance) {
        String sql = "UPDATE card SET balance = balance + ? WHERE number == ? AND pin == ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setBigDecimal(1, balance);
            pstmt.setString(2, acc.getCardNumber());
            pstmt.setString(3, acc.getCardPIN());
            pstmt.executeUpdate();
            con.commit();
            acc.setBalance(balance);
        } catch (SQLException e) {
            try {
                System.err.println("Couldn't add income. Rolling back changes");
                con.rollback();
            } catch (SQLException rollbackE) {
                rollbackE.printStackTrace();
            }
        }
    }

    private void closeAccount(Connection con, Account acc) {
        String sql = "DELETE FROM card WHERE number == ? AND pin == ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, acc.getCardNumber());
            pstmt.setString(2, acc.getCardPIN());
            pstmt.executeUpdate();
            con.commit();
            System.out.println("The account has been closed!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void transfer(Connection con, Account acc) {
        String targetAccount;
        System.out.println("Transfer \n Enter card number: ");
        targetAccount = scanner.next();
        if (targetAccount.equals(extractFromDataBase(con, "number", targetAccount))) {
            System.out.println("Enter how much money you want to transfer: ");
            double transferAmount = scanner.nextDouble();
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
                } catch (SQLException e) {
                    try {
                        System.err.println("Couldn't transfer money. Rolling back changes.");
                        con.rollback();
                    } catch (SQLException rollbackE) {
                        rollbackE.printStackTrace();
                    }
                }
            } else {
                System.out.println("Not enough money!");
            }
        } else if(!checkForLuhn(targetAccount)) {
            System.out.println("Probably you made mistake in the card number. Please try again!");
        } else {
            System.out.println("Such a card does not exist.");
        }

    }
}
