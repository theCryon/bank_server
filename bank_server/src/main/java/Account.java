import java.math.BigDecimal;

class Account {
    private String cardNumber;
    private String cardPIN;
    private BigDecimal balance;

    public Account() {
    }

    public Account(String cardNumber, String cardPIN, BigDecimal balance) {
        this.cardNumber = cardNumber;
        this.cardPIN = cardPIN;
        this.balance = balance;
    }

    public String getCardPIN() {
        return cardPIN;
    }

    public void setCardPIN(String cardPIN) {
        this.cardPIN = cardPIN;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal accountBalance) {
        this.balance = accountBalance;
    }
}

