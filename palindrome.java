import java.util.Scanner;

class Palindrome {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter a text: ");
        String str = sc.nextLine();

        String rev = "";

        for (int i = str.length() - 1; i >= 0; i--) {
            rev = rev + str.charAt(i);
        }

        if (str.equalsIgnoreCase(rev)) {
            System.out.println("The text is a palindrome");
        } else {
            System.out.println("The text is not a palindrome");
        }
    }
}