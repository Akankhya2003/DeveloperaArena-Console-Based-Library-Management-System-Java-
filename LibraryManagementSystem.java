import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Console-based Library Management System
 * Features:
 *  - Book management: add, update, remove, search, list
 *  - Member management: add, remove, search, list
 *  - Borrowing and returning books with due date tracking
 *  - Simple CSV persistence (books.csv, members.csv, loans.csv)
 *
 * Save as LibraryManagementSystem.java
 */
public class LibraryManagementSystem {
    public static void main(String[] args) {
        Library library = new Library();
        library.loadAll(); // load data from CSVs if present
        library.runConsole();
    }
}

/* --------- Model classes --------- */
class Book {
    String id; // unique id
    String title;
    String author;
    int totalCopies;
    int availableCopies;

    Book(String id, String title, String author, int totalCopies) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.totalCopies = totalCopies;
        this.availableCopies = totalCopies;
    }

    String toCSV() {
        return escape(id) + "," + escape(title) + "," + escape(author) + "," + totalCopies + "," + availableCopies;
    }

    static Book fromCSV(String line) {
        String[] parts = splitCSV(line, 5);
        if (parts == null) return null;
        Book b = new Book(parts[0], parts[1], parts[2], Integer.parseInt(parts[3]));
        b.availableCopies = Integer.parseInt(parts[4]);
        return b;
    }

    static String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\"\"");
    }

    static String[] splitCSV(String line, int expected) {
        // very small CSV parser assuming no commas in fields (or double-quoted)
        // For simplicity we assume fields don't contain commas, or were escaped simply.
        String[] arr = line.split(",", -1);
        if (arr.length < expected) return null;
        return arr;
    }
}

class Member {
    String id; // unique
    String name;
    String email;

    Member(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    String toCSV() {
        return escape(id) + "," + escape(name) + "," + escape(email);
    }

    static Member fromCSV(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length < 3) return null;
        return new Member(parts[0], parts[1], parts[2]);
    }

    static String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\"\"");
    }
}

class Loan {
    String loanId;
    String bookId;
    String memberId;
    LocalDate borrowDate;
    LocalDate dueDate;
    LocalDate returnDate; // null if not returned

    Loan(String loanId, String bookId, String memberId, LocalDate borrowDate, LocalDate dueDate) {
        this.loanId = loanId;
        this.bookId = bookId;
        this.memberId = memberId;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = null;
    }

    String toCSV() {
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        return loanId + "," + bookId + "," + memberId + "," + borrowDate.format(fmt) + "," + dueDate.format(fmt) + "," + (returnDate == null ? "" : returnDate.format(fmt));
    }

    static Loan fromCSV(String line) {
        String[] p = line.split(",", -1);
        if (p.length < 6) return null;
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        Loan l = new Loan(p[0], p[1], p[2], LocalDate.parse(p[3], fmt), LocalDate.parse(p[4], fmt));
        if (p[5] != null && p[5].trim().length() > 0) {
            l.returnDate = LocalDate.parse(p[5], fmt);
        }
        return l;
    }
}

/* --------- Library core --------- */
class Library {
    Scanner sc = new Scanner(System.in);
    ArrayList<Book> books = new ArrayList<Book>();
    ArrayList<Member> members = new ArrayList<Member>();
    ArrayList<Loan> loans = new ArrayList<Loan>();

    final String BOOKS_FILE = "books.csv";
    final String MEMBERS_FILE = "members.csv";
    final String LOANS_FILE = "loans.csv";

    // Main console loop
    void runConsole() {
        while (true) {
            printMainMenu();
            String choice = sc.nextLine().trim();
            if (choice.equals("0")) {
                saveAll();
                System.out.println("Data saved. Exiting. Goodbye!");
                break;
            }
            handleMainChoice(choice);
        }
    }

    void printMainMenu() {
        System.out.println("\n=== Library Management System ===");
        System.out.println("1. Book Management");
        System.out.println("2. Member Management");
        System.out.println("3. Transactions (Borrow/Return)");
        System.out.println("4. Reports / Lists");
        System.out.println("0. Save & Exit");
        System.out.print("Choose option: ");
    }

    void handleMainChoice(String c) {
        switch (c) {
            case "1": bookManagementMenu(); break;
            case "2": memberManagementMenu(); break;
            case "3": transactionMenu(); break;
            case "4": reportsMenu(); break;
            default: System.out.println("Invalid choice.");
        }
    }

    /* ---------------- Book Management ---------------- */
    void bookManagementMenu() {
        while (true) {
            System.out.println("\n--- Book Management ---");
            System.out.println("1. Add Book");
            System.out.println("2. Update Book");
            System.out.println("3. Remove Book");
            System.out.println("4. Search Book by Title/Author/ID");
            System.out.println("5. List All Books");
            System.out.println("0. Back");
            System.out.print("Choice: ");
            String ch = sc.nextLine().trim();
            if (ch.equals("0")) return;
            switch (ch) {
                case "1": addBook(); break;
                case "2": updateBook(); break;
                case "3": removeBook(); break;
                case "4": searchBooks(); break;
                case "5": listBooks(); break;
                default: System.out.println("Invalid choice.");
            }
        }
    }

    void addBook() {
        System.out.print("Enter Book ID (unique): ");
        String id = sc.nextLine().trim();
        if (findBookById(id) != null) {
            System.out.println("Book with this ID already exists.");
            return;
        }
        System.out.print("Title: ");
        String title = sc.nextLine().trim();
        System.out.print("Author: ");
        String author = sc.nextLine().trim();
        System.out.print("Number of copies: ");
        int copies = readIntSafe(1);
        Book b = new Book(id, title, author, copies);
        books.add(b);
        System.out.println("Book added.");
    }

    void updateBook() {
        System.out.print("Enter Book ID to update: ");
        String id = sc.nextLine().trim();
        Book b = findBookById(id);
        if (b == null) {
            System.out.println("No book found.");
            return;
        }
        System.out.println("Current title: " + b.title + " | author: " + b.author + " | total: " + b.totalCopies + " | avail: " + b.availableCopies);
        System.out.print("New title (leave blank to keep): ");
        String title = sc.nextLine().trim();
        if (!title.isEmpty()) b.title = title;
        System.out.print("New author (leave blank to keep): ");
        String author = sc.nextLine().trim();
        if (!author.isEmpty()) b.author = author;
        System.out.print("New total copies (0 to keep): ");
        int total = readIntSafe(0);
        if (total > 0) {
            int lentOut = b.totalCopies - b.availableCopies;
            if (total < lentOut) {
                System.out.println("Cannot set total copies less than currently lent out (" + lentOut + "). Update aborted.");
            } else {
                b.totalCopies = total;
                b.availableCopies = total - lentOut;
                System.out.println("Total copies updated.");
            }
        }
    }

    void removeBook() {
        System.out.print("Enter Book ID to remove: ");
        String id = sc.nextLine().trim();
        Book b = findBookById(id);
        if (b == null) { System.out.println("No book found."); return; }
        int lentOut = b.totalCopies - b.availableCopies;
        if (lentOut > 0) {
            System.out.println("Book cannot be removed; " + lentOut + " copies are currently lent out.");
            return;
        }
        for (int i = 0; i < books.size(); i++) {
            if (books.get(i).id.equals(id)) {
                books.remove(i);
                System.out.println("Book removed.");
                return;
            }
        }
    }

    void searchBooks() {
        System.out.print("Enter search keyword (title/author/id): ");
        String q = sc.nextLine().trim().toLowerCase();
        boolean found = false;
        for (int i = 0; i < books.size(); i++) {
            Book b = books.get(i);
            if (b.id.toLowerCase().contains(q) || b.title.toLowerCase().contains(q) || b.author.toLowerCase().contains(q)) {
                printBookRow(b);
                found = true;
            }
        }
        if (!found) System.out.println("No matching books.");
    }

    void listBooks() {
        if (books.size() == 0) { System.out.println("No books in library."); return; }
        System.out.println("\nID | Title | Author | Total | Available");
        for (int i = 0; i < books.size(); i++) {
            printBookRow(books.get(i));
        }
    }

    void printBookRow(Book b) {
        System.out.println(b.id + " | " + b.title + " | " + b.author + " | " + b.totalCopies + " | " + b.availableCopies);
    }

    Book findBookById(String id) {
        for (int i = 0; i < books.size(); i++) {
            Book b = books.get(i);
            if (b.id.equals(id)) return b;
        }
        return null;
    }

    /* ---------------- Member Management ---------------- */
    void memberManagementMenu() {
        while (true) {
            System.out.println("\n--- Member Management ---");
            System.out.println("1. Add Member");
            System.out.println("2. Remove Member");
            System.out.println("3. Search Member by Name/ID");
            System.out.println("4. List Members");
            System.out.println("0. Back");
            System.out.print("Choice: ");
            String ch = sc.nextLine().trim();
            if (ch.equals("0")) return;
            switch (ch) {
                case "1": addMember(); break;
                case "2": removeMember(); break;
                case "3": searchMembers(); break;
                case "4": listMembers(); break;
                default: System.out.println("Invalid choice.");
            }
        }
    }

    void addMember() {
        System.out.print("Enter Member ID (unique): ");
        String id = sc.nextLine().trim();
        if (findMemberById(id) != null) {
            System.out.println("Member with this ID exists.");
            return;
        }
        System.out.print("Name: ");
        String name = sc.nextLine().trim();
        System.out.print("Email: ");
        String email = sc.nextLine().trim();
        members.add(new Member(id, name, email));
        System.out.println("Member added.");
    }

    void removeMember() {
        System.out.print("Enter Member ID to remove: ");
        String id = sc.nextLine().trim();
        Member m = findMemberById(id);
        if (m == null) { System.out.println("No member found."); return; }
        // Check outstanding loans
        for (int i = 0; i < loans.size(); i++) {
            Loan l = loans.get(i);
            if (l.memberId.equals(id) && l.returnDate == null) {
                System.out.println("Member has outstanding loans; cannot remove.");
                return;
            }
        }
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).id.equals(id)) {
                members.remove(i);
                System.out.println("Member removed.");
                return;
            }
        }
    }

    void searchMembers() {
        System.out.print("Enter search keyword (name/id): ");
        String q = sc.nextLine().trim().toLowerCase();
        boolean found = false;
        for (int i = 0; i < members.size(); i++) {
            Member m = members.get(i);
            if (m.id.toLowerCase().contains(q) || m.name.toLowerCase().contains(q)) {
                System.out.println(m.id + " | " + m.name + " | " + m.email);
                found = true;
            }
        }
        if (!found) System.out.println("No matching members.");
    }

    void listMembers() {
        if (members.size() == 0) { System.out.println("No members."); return; }
        System.out.println("\nID | Name | Email");
        for (int i = 0; i < members.size(); i++) {
            Member m = members.get(i);
            System.out.println(m.id + " | " + m.name + " | " + m.email);
        }
    }

    Member findMemberById(String id) {
        for (int i = 0; i < members.size(); i++) {
            Member m = members.get(i);
            if (m.id.equals(id)) return m;
        }
        return null;
    }

    /* ---------------- Transactions ---------------- */
    void transactionMenu() {
        while (true) {
            System.out.println("\n--- Transactions ---");
            System.out.println("1. Borrow Book");
            System.out.println("2. Return Book");
            System.out.println("3. List Active Loans");
            System.out.println("4. List All Loans");
            System.out.println("0. Back");
            System.out.print("Choice: ");
            String ch = sc.nextLine().trim();
            if (ch.equals("0")) return;
            switch (ch) {
                case "1": borrowBook(); break;
                case "2": returnBook(); break;
                case "3": listActiveLoans(); break;
                case "4": listAllLoans(); break;
                default: System.out.println("Invalid choice.");
            }
        }
    }

    void borrowBook() {
        System.out.print("Member ID: ");
        String mid = sc.nextLine().trim();
        Member m = findMemberById(mid);
        if (m == null) { System.out.println("Member not found."); return; }
        System.out.print("Book ID: ");
        String bid = sc.nextLine().trim();
        Book b = findBookById(bid);
        if (b == null) { System.out.println("Book not found."); return; }
        if (b.availableCopies <= 0) { System.out.println("No copies available."); return; }
        // create loan
        LocalDate today = LocalDate.now();
        LocalDate due = today.plusDays(14); // 2-week loan
        String loanId = "L" + (loans.size() + 1) + "-" + System.currentTimeMillis();
        Loan loan = new Loan(loanId, bid, mid, today, due);
        loans.add(loan);
        b.availableCopies -= 1;
        System.out.println("Book borrowed. Due date: " + due.toString());
    }

    void returnBook() {
        System.out.print("Loan ID or Book ID to return: ");
        String q = sc.nextLine().trim();
        Loan foundLoan = null;
        for (int i = 0; i < loans.size(); i++) {
            Loan l = loans.get(i);
            if ( (l.loanId.equals(q) || l.bookId.equals(q)) && l.returnDate == null) {
                foundLoan = l;
                break;
            }
        }
        if (foundLoan == null) {
            System.out.println("No active loan found with that ID or Book ID.");
            return;
        }
        foundLoan.returnDate = LocalDate.now();
        Book b = findBookById(foundLoan.bookId);
        if (b != null) b.availableCopies += 1;
        System.out.println("Book returned on " + foundLoan.returnDate.toString());
        if (foundLoan.returnDate.isAfter(foundLoan.dueDate)) {
            long daysLate = foundLoan.returnDate.toEpochDay() - foundLoan.dueDate.toEpochDay();
            System.out.println("Returned late by " + daysLate + " day(s).");
        }
    }

    void listActiveLoans() {
        boolean found = false;
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        for (int i = 0; i < loans.size(); i++) {
            Loan l = loans.get(i);
            if (l.returnDate == null) {
                System.out.println(l.loanId + " | Book:" + l.bookId + " | Member:" + l.memberId + " | Borrowed:" + l.borrowDate.format(fmt) + " | Due:" + l.dueDate.format(fmt));
                found = true;
            }
        }
        if (!found) System.out.println("No active loans.");
    }

    void listAllLoans() {
        if (loans.size() == 0) { System.out.println("No loans yet."); return; }
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        for (int i = 0; i < loans.size(); i++) {
            Loan l = loans.get(i);
            System.out.println(l.loanId + " | Book:" + l.bookId + " | Member:" + l.memberId + " | Borrowed:" + l.borrowDate.format(fmt) + " | Due:" + l.dueDate.format(fmt) + " | Returned:" + (l.returnDate == null ? "-" : l.returnDate.format(fmt)));
        }
    }

    /* ---------------- Reports ---------------- */
    void reportsMenu() {
        while (true) {
            System.out.println("\n--- Reports / Lists ---");
            System.out.println("1. Books with low availability");
            System.out.println("2. Overdue loans");
            System.out.println("3. Member loan history");
            System.out.println("0. Back");
            System.out.print("Choice: ");
            String ch = sc.nextLine().trim();
            if (ch.equals("0")) return;
            switch (ch) {
                case "1": reportLowAvailability(); break;
                case "2": reportOverdueLoans(); break;
                case "3": memberLoanHistory(); break;
                default: System.out.println("Invalid choice.");
            }
        }
    }

    void reportLowAvailability() {
        System.out.print("Threshold for available copies (e.g., 1): ");
        int thresh = readIntSafe(1);
        boolean found = false;
        for (int i = 0; i < books.size(); i++) {
            Book b = books.get(i);
            if (b.availableCopies <= thresh) {
                printBookRow(b);
                found = true;
            }
        }
        if (!found) System.out.println("No books below threshold.");
    }

    void reportOverdueLoans() {
        LocalDate today = LocalDate.now();
        boolean found = false;
        for (int i = 0; i < loans.size(); i++) {
            Loan l = loans.get(i);
            if (l.returnDate == null && l.dueDate.isBefore(today)) {
                System.out.println(l.loanId + " | Book:" + l.bookId + " | Member:" + l.memberId + " | Due:" + l.dueDate.toString());
                found = true;
            }
        }
        if (!found) System.out.println("No overdue loans.");
    }

    void memberLoanHistory() {
        System.out.print("Enter Member ID: ");
        String id = sc.nextLine().trim();
        Member m = findMemberById(id);
        if (m == null) { System.out.println("Member not found."); return; }
        boolean found = false;
        for (int i = 0; i < loans.size(); i++) {
            Loan l = loans.get(i);
            if (l.memberId.equals(id)) {
                System.out.println(l.loanId + " | Book:" + l.bookId + " | Borrowed:" + l.borrowDate + " | Due:" + l.dueDate + " | Returned:" + (l.returnDate == null ? "-" : l.returnDate));
                found = true;
            }
        }
        if (!found) System.out.println("No loan history for this member.");
    }

    /* ---------------- Persistence ---------------- */
    void loadAll() {
        loadBooks();
        loadMembers();
        loadLoans();
    }

    void saveAll() {
        saveBooks();
        saveMembers();
        saveLoans();
    }

    void loadBooks() {
        File f = new File(BOOKS_FILE);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                Book b = Book.fromCSV(line);
                if (b != null) books.add(b);
            }
            System.out.println("Loaded " + books.size() + " books.");
        } catch (Exception e) {
            System.out.println("Failed to load books: " + e.getMessage());
        }
    }

    void saveBooks() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(BOOKS_FILE))) {
            for (int i = 0; i < books.size(); i++) {
                pw.println(books.get(i).toCSV());
            }
            //System.out.println("Books saved.");
        } catch (Exception e) {
            System.out.println("Failed to save books: " + e.getMessage());
        }
    }

    void loadMembers() {
        File f = new File(MEMBERS_FILE);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                Member m = Member.fromCSV(line);
                if (m != null) members.add(m);
            }
            System.out.println("Loaded " + members.size() + " members.");
        } catch (Exception e) {
            System.out.println("Failed to load members: " + e.getMessage());
        }
    }

    void saveMembers() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(MEMBERS_FILE))) {
            for (int i = 0; i < members.size(); i++) {
                pw.println(members.get(i).toCSV());
            }
            //System.out.println("Members saved.");
        } catch (Exception e) {
            System.out.println("Failed to save members: " + e.getMessage());
        }
    }

    void loadLoans() {
        File f = new File(LOANS_FILE);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                Loan l = Loan.fromCSV(line);
                if (l != null) loans.add(l);
            }
            System.out.println("Loaded " + loans.size() + " loans.");
        } catch (Exception e) {
            System.out.println("Failed to load loans: " + e.getMessage());
        }
    }

    void saveLoans() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(LOANS_FILE))) {
            for (int i = 0; i < loans.size(); i++) {
                pw.println(loans.get(i).toCSV());
            }
            //System.out.println("Loans saved.");
        } catch (Exception e) {
            System.out.println("Failed to save loans: " + e.getMessage());
        }
    }

    /* ---------------- Utilities ---------------- */
    int readIntSafe(int minAllowed) {
        while (true) {
            String s = sc.nextLine().trim();
            if (s.length() == 0 && minAllowed == 0) return 0;
            try {
                int v = Integer.parseInt(s);
                if (v < minAllowed) {
                    System.out.print("Value must be >= " + minAllowed + ". Try again: ");
                    continue;
                }
                return v;
            } catch (Exception e) {
                System.out.print("Invalid number. Try again: ");
            }
        }
    }
}
