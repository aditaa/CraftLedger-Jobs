package dev.monkeycraft.craftledgerjobs;

import java.util.List;

final class PagedText {
    private PagedText() {
    }

    static String format(String title, List<String> rows, int requestedPage, int pageSize) {
        if (rows.isEmpty()) {
            return "No " + title.toLowerCase() + " configured.";
        }
        int safePageSize = Math.max(1, pageSize);
        int totalPages = Math.max(1, (int) Math.ceil(rows.size() / (double) safePageSize));
        int page = Math.max(1, Math.min(requestedPage, totalPages));
        int start = (page - 1) * safePageSize;
        int end = Math.min(start + safePageSize, rows.size());

        StringBuilder builder = new StringBuilder(title)
                .append(" (page ")
                .append(page)
                .append("/")
                .append(totalPages)
                .append("):");
        for (int index = start; index < end; index++) {
            builder.append("\n").append(rows.get(index));
        }
        return builder.toString();
    }
}
